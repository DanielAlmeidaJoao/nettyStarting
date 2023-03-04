package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.logics.StreamReceiverEOSFunction;
import org.example.logics.StreamReceiverFunction;

import java.io.FileOutputStream;
import java.net.InetSocketAddress;

public class StreamReceiver {
    private final int port;

    private Channel serverChannel;

    private StreamReceiverFunction streamReceiverFunction;
    private StreamReceiverEOSFunction streamReceiverEOSFunction;
    public StreamReceiver(int port,StreamReceiverFunction streamReceiverFunction,StreamReceiverEOSFunction streamReceiverEOSFunction ) {
        this.port = port;
        this.streamReceiverFunction = streamReceiverFunction;
        this.streamReceiverEOSFunction = streamReceiverEOSFunction;
    }

    public void startListening() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_RCVBUF, 64 * 1024)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new StreamReceiverHandler(streamReceiverFunction,streamReceiverEOSFunction));
                        }
                    });
            ChannelFuture f = b.bind().sync();
            serverChannel = f.channel();
            serverChannel.closeFuture().sync();
            System.out.println("HELLO!");
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public void close(){
        serverChannel.close();
    }
}
