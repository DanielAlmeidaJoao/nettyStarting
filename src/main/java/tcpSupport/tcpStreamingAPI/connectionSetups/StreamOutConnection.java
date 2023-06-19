package tcpSupport.tcpStreamingAPI.connectionSetups;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import tcpSupport.tcpStreamingAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamMetrics;
import tcpSupport.tcpStreamingAPI.pipeline.StreamSenderHandler;
import tcpSupport.tcpStreamingAPI.pipeline.encodings.DelimitedMessageDecoder;
import tcpSupport.tcpStreamingAPI.pipeline.encodings.StreamMessageDecoder;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

import static tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils.CUSTOM_ID_KEY;

public class StreamOutConnection {


    private EventLoopGroup group;
    public InetSocketAddress self;

    public StreamOutConnection(InetSocketAddress host) {
        group = createNewWorkerGroup(1);
        this.self = host;
    }

    public void connect(InetSocketAddress peer, TCPStreamMetrics metrics, StreamingNettyConsumer consumer, TransmissionType type, String conId){
        try {
            Bootstrap b = new Bootstrap();

            b.group(group)
                    .channel(socketChannel())
                    .remoteAddress(peer)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                            if(TransmissionType.STRUCTURED_MESSAGE==type){
                                ch.pipeline().addLast(new DelimitedMessageDecoder(metrics, consumer));
                            }else{
                                ch.pipeline().addLast(new StreamMessageDecoder(metrics,consumer));
                            }
                            ch.pipeline().addLast( new StreamSenderHandler(new HandShakeMessage(self,type),consumer,metrics,type));
                    }
                    }).attr(AttributeKey.valueOf(CUSTOM_ID_KEY),conId);

            b.connect().addListener(future -> {
                if(!future.isSuccess()){
                    consumer.handleOpenConnectionFailed(peer,future.cause());
                }
            });
            //printSomeConfigs();
            /***
            updateConfiguration(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK,64*1024);
            updateConfiguration(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK,2*64*1024);
            updateConfiguration(ChannelOption.AUTO_READ,Boolean.TRUE);**/
        } catch (Exception e) {
            consumer.handleOpenConnectionFailed(peer,e.getCause());
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
