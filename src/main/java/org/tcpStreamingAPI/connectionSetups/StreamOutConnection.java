package org.tcpStreamingAPI.connectionSetups;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.tcpStreamingAPI.channel.StreamingNettyConsumer;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import org.tcpStreamingAPI.pipeline.StreamSenderHandler;
import org.tcpStreamingAPI.pipeline.encodings.DelimitedMessageDecoder;
import org.tcpStreamingAPI.pipeline.encodings.DelimitedMessageEncoder;

import java.net.InetSocketAddress;
public class StreamOutConnection {


    private HandShakeMessage handShakeMessage;
    private EventLoopGroup group;

    public StreamOutConnection(InetSocketAddress host) {
        group = createNewWorkerGroup(1);
        handShakeMessage = new HandShakeMessage(host);
    }

    public void connect(InetSocketAddress peer,TCPStreamMetrics metrics, StreamingNettyConsumer consumer){
        try {
            Bootstrap b = new Bootstrap();

            b.group(group)
                    .channel(socketChannel())
                    .remoteAddress(peer)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                            ch.pipeline().addLast(new DelimitedMessageEncoder());
                            ch.pipeline().addLast(new DelimitedMessageDecoder(metrics));
                            ch.pipeline().addLast( new StreamSenderHandler(handShakeMessage,consumer,metrics));
                    }
                    });

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

}
