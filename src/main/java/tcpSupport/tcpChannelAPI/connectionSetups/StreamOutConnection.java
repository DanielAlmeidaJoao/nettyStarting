package tcpSupport.tcpChannelAPI.connectionSetups;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.pipeline.TCPClientNettyHandler;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPDelimitedMessageDecoder;
import tcpSupport.tcpChannelAPI.pipeline.encodings.TCPStreamMessageDecoder;
import tcpSupport.tcpChannelAPI.utils.TCPStreamUtils;

import java.net.InetSocketAddress;
import java.util.Properties;

import static tcpSupport.tcpChannelAPI.utils.TCPStreamUtils.CUSTOM_ID_KEY;

public class StreamOutConnection {


    private EventLoopGroup group;
    public InetSocketAddress self;
    private final int connectionTimeout;
    public StreamOutConnection(InetSocketAddress host,Properties properties) {
        group = createNewWorkerGroup(1);
        this.self = host;
        connectionTimeout = Integer.parseInt((String) properties.getOrDefault(TCPStreamUtils.CONNECT_TIMEOUT_MILLIS,"30000"));

    }

    public void connect(InetSocketAddress peer, StreamingNettyConsumer consumer, TransmissionType type, String conId){
        try {
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
                            ch.pipeline().addLast( new TCPClientNettyHandler(new HandShakeMessage(self,type),consumer,type));
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
        } catch (Exception e) {
            consumer.handleOpenConnectionFailed(peer,e.getCause(), type,conId);
        }
    }

    public static EventLoopGroup createNewWorkerGroup(int nThreads) {
        //if (Epoll.isAvailable()) return new EpollEventLoopGroup(nThreads);
        //else
        return new NioEventLoopGroup(nThreads);
    }
    private Class<? extends Channel> socketChannel(){
        /**
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        }**/
        return NioSocketChannel.class;
    }
    public void shutDown(){
        if(group!=null){
            group.shutdownGracefully();
        }
    }

}
