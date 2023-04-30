package udpSupport.client_server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
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

import static udpSupport.client_server.NettyUDPServer.BUFFER_SIZE;

public class NettyUDPClient {

    public DatagramChannel start(UDPChannelConsumer consumer)throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(BUFFER_SIZE))
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //pipeline.addLast(new UDPMessageEncoder());
                            pipeline.addLast(new ClientHandler(consumer));
                        }
                    });

            return (DatagramChannel) b.bind(0).sync().channel();
        } finally {
            group.shutdownGracefully();
        }
    }
}
