package tcpSupport.tcpChannelAPI.connectionSetups;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.pipeline.TCPClientNettyHandler;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPDelimitedMessageDecoder;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPStreamMessageDecoder;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;

import java.net.InetSocketAddress;
import java.util.Properties;

import static java.util.concurrent.TimeUnit.SECONDS;
import static tcpSupport.tcpChannelAPI.utils.TCPChannelUtils.CUSTOM_ID_KEY;
import static tcpSupport.tcpChannelAPI.utils.TCPChannelUtils.TCP_IDLE_CONNECTION_TIMEOUT;

public class TCPClientEntity implements ClientInterface{


    private final EventLoopGroup group;
    public InetSocketAddress self;
    private final int connectionTimeout;
    private final StreamingNettyConsumer consumer;
    private final int idleConnectionTimeout;
    public TCPClientEntity(InetSocketAddress host, Properties properties, StreamingNettyConsumer consumer) {
        group = TCPServerEntity.createNewWorkerGroup(TCPChannelUtils.clientThreads(properties));
        this.self = host;
        this.consumer = consumer;
        connectionTimeout = Integer.parseInt((String) properties.getOrDefault(TCPChannelUtils.CONNECT_TIMEOUT_MILLIS,"30000"));
        idleConnectionTimeout = Integer.parseInt(properties.getProperty(TCP_IDLE_CONNECTION_TIMEOUT,"-1"));
    }

    //    public void connect(InetSocketAddress remote, Properties properties, TransmissionType transmissionType, String id) throws Exception{
    public void connect(InetSocketAddress peer, TransmissionType type, String conId, short destProto) throws Exception{
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(socketChannel())
                .remoteAddress(peer)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,connectionTimeout)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        if(TransmissionType.STRUCTURED_MESSAGE==type){
                            ch.pipeline().addLast(TCPDelimitedMessageDecoder.NAME,new TCPDelimitedMessageDecoder(consumer));
                        }else{
                            ch.pipeline().addLast(TCPStreamMessageDecoder.NAME,new TCPStreamMessageDecoder(consumer));
                        }
                        if(idleConnectionTimeout>0){
                            ch.pipeline().addLast(new IdleStateHandler(0, 0, idleConnectionTimeout, SECONDS));
                        }
                        ch.pipeline().addLast( new TCPClientNettyHandler(new HandShakeMessage(self,type,destProto),consumer,type));
                    }
                }).attr(AttributeKey.valueOf(CUSTOM_ID_KEY),conId);

        b.connect().addListener(future -> {
            if(!future.isSuccess()){
                consumer.handleOpenConnectionFailed(peer,future.cause(), type,conId);
            }
        });
        //printSomeConfigs();
        /***
         updateConfiguration(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK,64*1024);
         updateConfiguration(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK,2*64*1024);
         updateConfiguration(ChannelOption.AUTO_READ,Boolean.TRUE);**/
    }


    private Class<? extends Channel> socketChannel(){
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        }else{
            return NioSocketChannel.class;
        }
    }
    public void shutDown(){
        group.shutdownGracefully();
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return group;
    }

}
