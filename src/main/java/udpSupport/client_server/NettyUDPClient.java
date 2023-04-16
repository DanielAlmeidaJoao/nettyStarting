package udpSupport.client_server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.DatagramPacketEncoder;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.pipeline.ClientHandler;
import udpSupport.pipeline.UDPMessageEncoder;

import java.net.InetSocketAddress;
import java.util.Scanner;

public class NettyUDPClient {

    public void start(UDPChannelConsumer consumer)throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //pipeline.addLast(new UDPMessageEncoder());
                            pipeline.addLast(new ClientHandler(consumer));
                        }
                    });

            DatagramChannel channel = (DatagramChannel) b.bind(0).sync().channel();
            System.out.println("CLIENT STARTED!!!");
            InetSocketAddress dest =  new InetSocketAddress("localhost", 9999);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("ENTER SOMETHING: ");
                String line = scanner.nextLine();
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                int times = Integer.parseInt(line);
                line = "a".repeat(times);
                DatagramPacket datagramPacket = new DatagramPacket(Unpooled.copiedBuffer(line.getBytes()),dest);
                channel.writeAndFlush(datagramPacket);
                System.out.println("DATA SENT@ "+line.length());
            }
        } finally {
            group.shutdownGracefully();
        }
    }
    public static void main(String[] args) throws Exception {
        NettyUDPClient nettyUDPClient = new NettyUDPClient();
        nettyUDPClient.start(null);
    }

}
