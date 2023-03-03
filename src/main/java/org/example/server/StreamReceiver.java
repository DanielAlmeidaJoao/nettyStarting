package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.FileOutputStream;
import java.net.InetSocketAddress;

public class StreamReceiver {
    private final int port;
    private FileOutputStream fos;
    public StreamReceiver(int port) {
        this.port = port;
        try {
            //String inputFileName = "/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4";
            fos = new FileOutputStream("ola2_movie.mp4");
        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println(
                    "Usage: " + StreamReceiver.class.getSimpleName() +
                            " <port>");
        }
        int port = Integer.parseInt(args[0]);
        new StreamReceiver(port).start();
    }
    public void start() throws Exception {
        final StreamReceiverHandler serverHandler = new StreamReceiverHandler(this::writeToFile,this::closeFile);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(serverHandler);
                        }
                    });
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    private void writeToFile(String id, byte [] data){
        try{
            fos.write(data, 0, data.length);
            fos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void closeFile(String id){
        System.out.println("CONNECTION CLOSED: "+id);
        try{
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
