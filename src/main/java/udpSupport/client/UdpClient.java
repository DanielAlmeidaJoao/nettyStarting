package udpSupport.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;

public class UdpClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioDatagramChannel.class)
             .option(ChannelOption.SO_BROADCAST, true)
             .handler(new ChannelInitializer<DatagramChannel>() {
                @Override
                public void initChannel(DatagramChannel ch) throws Exception {
                    ch.pipeline().addLast(new UdpClientHandler());
                }
             });

            DatagramChannel ch = (DatagramChannel) b.bind(0).sync().channel();
            DatagramChannel channel = (DatagramChannel) b.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT)).sync().channel();

            ByteBuf data = ch.alloc().buffer();
            data.writeBytes("Hello, server!".getBytes());
            DatagramPacket dp = new DatagramPacket(data, new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            ch.writeAndFlush(dp).sync();
            dp = new DatagramPacket(Unpooled.copiedBuffer("OLLAAA JOIHN ".getBytes()), new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            channel.writeAndFlush(dp).sync();
            ch.closeFuture().await();
        }catch (Exception e){
          e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }


}
