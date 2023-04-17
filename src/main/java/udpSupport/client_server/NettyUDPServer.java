package udpSupport.client_server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.utils.Logics;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.metrics.ChannelStats;
import udpSupport.pipeline.InMessageHandler;
import udpSupport.utils.funcs.OnAckFunction;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyUDPServer {
    private static final Logger logger = LogManager.getLogger(NettyUDPServer.class);

    private Set<Long> waitingForAcks;
    private AtomicLong datagramPacketCounter;
    private final Channel channel;
    private final UDPChannelConsumer consumer;
    private final InetSocketAddress address;
    private final ChannelStats stats;

    public NettyUDPServer(UDPChannelConsumer consumer, ChannelStats stats, InetSocketAddress address) throws Exception {
        this.stats = stats;
        this.consumer = consumer;
        this.address = address;
        channel = start();
        waitingForAcks = new ConcurrentSkipListSet<>();
        datagramPacketCounter = new AtomicLong(0);
    }
    private void scheduleRetransmission(byte[] packet, long msgId, InetSocketAddress dest, int count){
        channel.eventLoop().schedule(() -> {
            if(!waitingForAcks.contains(msgId)) return;
            if(count > 100){waitingForAcks.remove(msgId);System.out.println(" PEER NOT RESPONGING "+dest); ;return;}
            channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(packet),dest)).addListener(future -> {
                if(future.isSuccess()){
                    if(stats!=null){
                        stats.addSentBytes(dest,packet.length,UDPLogics.APP_MESSAGE);
                    }
                }else{
                    future.cause().printStackTrace();
                }
                scheduleRetransmission(packet,msgId,dest,count+1);
            });
        },5, TimeUnit.SECONDS);
        if(count==0){
            waitingForAcks.add(msgId);
        }
    }
    public void onAckReceived(long msgId){
        waitingForAcks.remove(msgId);
    }
    private Channel start() throws Exception{
        OnAckFunction onAckReceived = this::onAckReceived;
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
                        pipeline.addLast(new InMessageHandler(consumer,stats,onAckReceived));
                    }
                });
        server = b.bind(address).sync().channel();
        logger.info("UDP SERVER LISTENING ON : {}",address);
        return server;
    }

    public void sendMessage(byte [] message, InetSocketAddress peer, int len){
        long c = datagramPacketCounter.incrementAndGet();
        ByteBuf buf = Unpooled.buffer(message.length+9);
        buf.writeByte(UDPLogics.APP_MESSAGE);
        buf.writeLong(c);
        buf.writeBytes(message,0, len);
        byte [] toResend = new byte[buf.readableBytes()];
        buf.markReaderIndex();
        buf.readBytes(toResend);
        buf.resetReaderIndex();
        DatagramPacket datagramPacket = new DatagramPacket(buf,peer);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(future.isSuccess()){
                scheduleRetransmission(toResend,c,peer,0);
                if(stats!=null){
                    stats.addSentBytes(peer,toResend.length,UDPLogics.APP_MESSAGE);
                }
            }
            consumer.messageSentHandler(future.isSuccess(),future.cause(),message,peer);
        });
    }

    public void send(){
        InetSocketAddress dest =  new InetSocketAddress("localhost", 9999);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("ENTER SOMETHING: ");
            String line = scanner.nextLine();
            if(line.equalsIgnoreCase("m")){
                System.out.println(Logics.gson.toJson(stats));
                continue;
            }
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            int times = Integer.parseInt(line);
            line = "a".repeat(times);
            sendMessage(line.getBytes(),dest,line.length());
            System.out.println("DATA SENT@ "+line.length());
        }
    }

    public static void main(String[] args) throws Exception {
        NettyUDPServer nettyUDPServer = new NettyUDPServer(null,new ChannelStats(),null);
        nettyUDPServer.send();

    }

}
