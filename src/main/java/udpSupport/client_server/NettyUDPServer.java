package udpSupport.client_server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.metrics.ChannelStats;
import udpSupport.pipeline.InMessageHandler;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;

public class NettyUDPServer {
    private final Channel channel;
    private final UDPChannelConsumer consumer;
    private final ChannelStats stats;

    public NettyUDPServer(UDPChannelConsumer consumer, ChannelStats stats) throws Exception {
        this.stats = stats;
        channel = start();
        this.consumer = consumer;
    }


    private Channel start() throws Exception{
        Channel server;
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1024*65))
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new InMessageHandler(consumer,stats));
                    }
                });
        server = b.bind(new InetSocketAddress(9999)).sync().channel();
        return server;
    }
    public void sendMessage(byte [] message, InetSocketAddress peer){
        ByteBuf buf = Unpooled.buffer(message.length+1);
        buf.writeByte(UDPLogics.APP_MESSAGE);
        buf.writeBytes(message);
        DatagramPacket datagramPacket = new DatagramPacket(buf,peer);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            consumer.messageSentHandler(future.isSuccess(),future.cause(),message,peer);
        });
    }
    public static void main(String[] args) throws Exception {
        //NettyUDPServer nettyUDPServer = new NettyUDPServer(null);

    }
}
