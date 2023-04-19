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
import udpSupport.metrics.NetworkStatsKindEnum;
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
    private final AtomicLong datagramPacketCounter;
    private final AtomicLong streamIdCounter;

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
        streamIdCounter = new AtomicLong(0);
    }
    private void scheduleRetransmission(byte[] packet, long msgId, InetSocketAddress dest, int count){
        channel.eventLoop().schedule(() -> {
            if(!waitingForAcks.contains(msgId)) return;
            if(count > 100){waitingForAcks.remove(msgId);System.out.println(" PEER NOT RESPONGING "+dest); ;return;}
            channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(packet),dest)).addListener(future -> {
                if(future.isSuccess()){
                    if(stats!=null){
                        stats.addSentBytes(dest,packet.length, NetworkStatsKindEnum.MESSAGE_STATS);
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
        if(UDPLogics.MAX_UDP_PAYLOAD_SIZE<message.length){
            long streamId = streamIdCounter.incrementAndGet();
            ByteBuf wholeMessageBuf = Unpooled.copiedBuffer(message);
            int streamCount = ceilDiv(message.length,UDPLogics.MAX_UDP_PAYLOAD_SIZE); //do the %
            for (int i = 0; i < streamCount; i++) {
                long messageId = datagramPacketCounter.incrementAndGet();
                int streamLen = Math.min(wholeMessageBuf.readableBytes(), UDPLogics.MAX_UDP_PAYLOAD_SIZE);
                byte [] stream = new byte[streamLen];
                wholeMessageBuf.readBytes(stream);
                ByteBuf byteBuf = Unpooled.buffer(/* Byte.BYTES+Long.BYTES*2+Integer.BYTES+streamLen */);
                byteBuf.writeByte(UDPLogics.STREAM_MESSAGE);
                byteBuf.writeLong(messageId);
                byteBuf.writeLong(streamId);
                byteBuf.writeInt(streamCount);
                byteBuf.writeBytes(stream);
                //sendMessageAux(byteBuf,peer,messageId);
                //byteBuf.release();
            }
            wholeMessageBuf.release();
        }else{
            long messageId = datagramPacketCounter.incrementAndGet();
            ByteBuf buf = Unpooled.buffer(9+len);
            buf.writeByte(UDPLogics.SINGLE_MESSAGE);
            buf.writeLong(messageId);
            buf.writeBytes(message,0, len);
            byte [] all = new byte [buf.readableBytes()];
            buf.readBytes(all);
            buf.release();
            sendMessageAux(all,peer,messageId);
            //buf.release();

        }
    }
    private int ceilDiv(int a, int b){
        if(a%b==0){
            return a/b;
        }else{
            return (a/b) + 1;
        }
    }
    public void sendMessageAux(byte [] all, InetSocketAddress peer,long messageId){
        ByteBuf buf = Unpooled.copiedBuffer(all);
        byte [] toResend = new byte[buf.readableBytes()];
        buf.markReaderIndex();
        buf.readBytes(toResend);
        buf.resetReaderIndex();
        DatagramPacket datagramPacket = new DatagramPacket(buf,peer);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(future.isSuccess()){
                scheduleRetransmission(toResend,messageId,peer,0);
                if(stats!=null){
                    stats.addSentBytes(peer,toResend.length,NetworkStatsKindEnum.MESSAGE_STATS);
                }
            }
            consumer.messageSentHandler(future.isSuccess(),future.cause(),null /*TODO message */,peer);
        });
    }
    public void sendMessage(byte msgType,byte [] message, InetSocketAddress peer, int len){
        //TODO : REMOVE
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
