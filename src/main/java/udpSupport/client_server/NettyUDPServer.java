package udpSupport.client_server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import quicSupport.utils.Logics;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.metrics.ChannelStats;
import udpSupport.pipeline.InMessageHandler;
import udpSupport.pipeline.UDPMessageEncoder;
import udpSupport.utils.Pair;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyUDPServer implements UDPChannelConsumer{
    private Map<Long,Pair<DatagramPacket,ScheduledFuture>> waitingForAcks;
    private AtomicLong datagramPacketCounter;
    private final Channel channel;
    private final UDPChannelConsumer consumer;
    private final ChannelStats stats;

    public NettyUDPServer(UDPChannelConsumer consumer, ChannelStats stats) throws Exception {
        this.stats = stats;
        channel = start();
        this.consumer = this;
        waitingForAcks = new HashMap<>();
        datagramPacketCounter = new AtomicLong(0);
    }
    private void scheduleRetransmission(DatagramPacket packet, long msgId){
        ScheduledFuture scheduledFuture = channel.eventLoop().schedule(() -> {
            if(!waitingForAcks.containsKey(msgId)) return;
            channel.writeAndFlush(packet).addListener(future -> {
                if(future.isSuccess()){
                    scheduleRetransmission(packet,msgId);
                }
            });
        },30, TimeUnit.SECONDS);
        Pair<DatagramPacket,ScheduledFuture> pair = waitingForAcks.get(msgId);
        if(pair==null){
            pair = new Pair<>(packet,scheduledFuture);
            waitingForAcks.put(msgId,pair);
        }else {
            pair.setRight(scheduledFuture);
        }
    }
    public void onAckReceived(long msgId){
        System.out.println("RECEIVED ACK FOR "+msgId);
        Pair<DatagramPacket,ScheduledFuture> p = waitingForAcks.remove(msgId);
        if(p!=null){
            p.getRight().cancel(true);
        }
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
                        //pipeline.addLast(new UDPMessageEncoder());
                        pipeline.addLast(new InMessageHandler(consumer,stats));
                    }
                });
        server = b.bind(new InetSocketAddress(9999)).sync().channel();
        System.out.println("SERVER STARTED !!!");
        return server;
    }

    public void sendMessage(byte [] message, InetSocketAddress peer){
        long c = datagramPacketCounter.incrementAndGet();
        ByteBuf buf = Unpooled.buffer(message.length+1);
        buf.writeByte(UDPLogics.APP_MESSAGE);
        buf.writeLong(c);
        buf.writeBytes(message);
        DatagramPacket datagramPacket = new DatagramPacket(buf,peer);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(future.isSuccess()){
                System.out.println("SENT "+c);
                scheduleRetransmission(datagramPacket,c);
            }
            consumer.messageSentHandler(future.isSuccess(),future.cause(),message,peer);
        });

    }
    @Override
    public void deliver(byte[] message, InetSocketAddress from) {
        System.out.println("RECEIVED MESSAGE "+message.length);
    }

    @Override
    public void deliverAck(long msgId) {
        onAckReceived(msgId);
    }

    @Override
    public void messageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest) {
        if(!success){
            error.printStackTrace();
        }
        System.out.println("SENT RESULT "+success + " err: ");
    }

    public void send(){
        InetSocketAddress dest =  new InetSocketAddress("localhost", 9999);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("ENTER SOMETHING: ");
            String line = scanner.nextLine();
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            int times = Integer.parseInt(line);
            line = "a".repeat(times);
            sendMessage(line.getBytes(),dest);
            System.out.println("DATA SENT@ "+line.length());
        }
    }
    public static void main(String[] args) throws Exception {
        NettyUDPServer nettyUDPServer = new NettyUDPServer(null,new ChannelStats());
        nettyUDPServer.send();

    }

}
