package udpSupport.client_server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.ScheduledFuture;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.metrics.ChannelStats;
import udpSupport.pipeline.InMessageHandler;
import udpSupport.utils.Pair;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyUDPServer implements UDPChannelConsumer{
    private Set<Long> waitingForAcks;
    private AtomicLong datagramPacketCounter;
    private final Channel channel;
    private final NettyUDPServer consumer;
    private final ChannelStats stats;

    public NettyUDPServer(UDPChannelConsumer consumer, ChannelStats stats) throws Exception {
        this.stats = stats;
        this.consumer = this;
        channel = start();
        waitingForAcks = new ConcurrentSkipListSet<>();
        datagramPacketCounter = new AtomicLong(0);
    }
    private void scheduleRetransmission(byte[] packet, long msgId, InetSocketAddress dest, boolean firstTime){
        ScheduledFuture scheduledFuture = channel.eventLoop().schedule(() -> {
            System.out.println("BLA BLA");
            if(!waitingForAcks.contains(msgId)) return;
            System.out.println("SCHEDULER TRIGGERED!!!");
            channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(packet),dest)).addListener(future -> {
                System.out.println(future.isSuccess()+" 99999999 "+msgId);
                if(!future.isSuccess()){
                    future.cause().printStackTrace();
                }
                scheduleRetransmission(packet,msgId,dest,false);
            });
        },5, TimeUnit.SECONDS);
        if(firstTime){
            waitingForAcks.add(msgId);
        }
        /**
        Pair pair = waitingForAcks.get(msgId);
        if(pair==null){
            pair = new Pair(packet,scheduledFuture);
            waitingForAcks.put(msgId,pair);
        }else {
            pair.setRight(scheduledFuture);
        }
         **/
        System.out.println("RETRANSMISSION CALLED!!!");
    }
    public void onAckReceived(long msgId){
        System.out.println("RECEIVED ACK FOR "+msgId);
        waitingForAcks.remove(msgId);
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
        byte [] toResend = new byte[buf.readableBytes()];
        buf.markReaderIndex();
        buf.readBytes(toResend);
        buf.resetReaderIndex();
        DatagramPacket datagramPacket = new DatagramPacket(buf,peer);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(future.isSuccess()){
                System.out.println("SENT "+c);
                scheduleRetransmission(toResend,c,peer,true);
            }
            consumer.messageSentHandler(future.isSuccess(),future.cause(),message,peer);
        });

    }

    public void deliver(byte[] message, InetSocketAddress from) {
        System.out.println("RECEIVED MESSAGE "+message.length);
    }

    public void deliverAck(long msgId) {
        System.out.println("PORRRAS ");
        onAckReceived(msgId);
    }

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
