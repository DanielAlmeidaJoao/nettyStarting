package org.streamingAPI.connectionSetups;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.streamingAPI.channel.StreamingNettyConsumer;
import org.streamingAPI.metrics.TCPStreamMetrics;
import org.streamingAPI.pipeline.StreamSenderHandler;
import org.streamingAPI.pipeline.encodings.DelimitedMessageDecoder;
import org.streamingAPI.pipeline.encodings.StreamMessageDecoder;
import org.streamingAPI.connectionSetups.messages.HandShakeMessage;
import java.net.InetSocketAddress;
public class StreamOutConnection {


    private HandShakeMessage handShakeMessage;
    private EventLoopGroup group;

    public StreamOutConnection(InetSocketAddress host) {
        group = createNewWorkerGroup(1);
        handShakeMessage = new HandShakeMessage(host);
    }

    public void connect(InetSocketAddress peer, boolean readDelimited, TCPStreamMetrics metrics, StreamingNettyConsumer consumer){
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(socketChannel())
                    .remoteAddress(peer)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                            if(readDelimited){
                                ch.pipeline().addLast(new DelimitedMessageDecoder(metrics));
                            }else{
                                ch.pipeline().addLast(new StreamMessageDecoder(metrics));
                            }
                        ch.pipeline().addLast( new StreamSenderHandler(handShakeMessage,consumer,metrics));
                    }
                    });
            b.connect().sync().addListener(future -> {
                if(!future.isSuccess()){
                    //TODO
                }
            }).channel();


            //printSomeConfigs();
            /***
            updateConfiguration(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK,64*1024);
            updateConfiguration(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK,2*64*1024);
            updateConfiguration(ChannelOption.AUTO_READ,Boolean.TRUE);**/
        } catch (Exception e) {

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

}
