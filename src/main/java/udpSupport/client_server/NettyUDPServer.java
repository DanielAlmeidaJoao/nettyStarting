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
import quicSupport.utils.QUICLogics;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.metrics.ChannelStats;
import udpSupport.metrics.NetworkStatsKindEnum;
import udpSupport.pipeline.InMessageHandler;
import udpSupport.utils.funcs.OnAckFunction;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyUDPServer {
    private static final Logger logger = LogManager.getLogger(NettyUDPServer.class);
    public static final int BUFFER_SIZE = 1024 * 65;
    public static final String UDP_RETRANSMISSION_TIMEOUT = "UDP_RETRANSMISSION_TIMEOUT";
    public static final String UDP_N_THREADS = "UDP_N_THREADS";
    public int RETRANSMISSION_TIMEOUT;
    public final int MAX_SEND_RETRIES;
    public static final String MAX_SEND_RETRIES_KEY = "UPD_MAX_SEND_RETRIES";


    private Map<Long,Long> waitingForAcks;
    private final AtomicLong datagramPacketCounter;
    private final AtomicLong streamIdCounter;

    private final Channel channel;
    private final UDPChannelConsumer consumer;
    private final InetSocketAddress address;
    private final ChannelStats stats;
    private final Properties properties;

    public NettyUDPServer(UDPChannelConsumer consumer, ChannelStats stats, InetSocketAddress address, Properties properties){
        this.properties=properties;
        this.stats = stats;
        this.consumer = consumer;
        this.address = address;
        try {
            channel = start();
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("UDP LISTENER COULD NOT START!");
        }
        waitingForAcks = new ConcurrentHashMap<>();
        datagramPacketCounter = new AtomicLong(0);
        streamIdCounter = new AtomicLong(0);
        MAX_SEND_RETRIES = Integer.parseInt(properties.getProperty(MAX_SEND_RETRIES_KEY,"5"));
        RETRANSMISSION_TIMEOUT = Integer.parseInt(properties.getProperty(UDP_RETRANSMISSION_TIMEOUT,"1"));
    }
    public void onAckReceived(long msgId, InetSocketAddress sender){
        Long timeMillis = waitingForAcks.remove(msgId);
        if(stats!=null&&timeMillis!=null){
            stats.addTransmissionRTT(sender,(System.currentTimeMillis() - timeMillis));
        }
    }
    private void scheduleRetransmission(byte[] packet, long msgId, InetSocketAddress dest, int count){
        channel.eventLoop().schedule(() -> {
            if(!waitingForAcks.containsKey(msgId)) return;
            if(count > MAX_SEND_RETRIES){waitingForAcks.remove(msgId);return;}
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
        }, RETRANSMISSION_TIMEOUT,TimeUnit.SECONDS);
        if(count==0){
            waitingForAcks.put(msgId,System.currentTimeMillis());
        }
    }

    private Channel start() throws Exception{
        OnAckFunction onAckReceived = this::onAckReceived;
        Channel server;
        int n_threads = Integer.parseInt(properties.getProperty(UDP_N_THREADS,"2"));
        EventLoopGroup group = new NioEventLoopGroup(n_threads);
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(BUFFER_SIZE))
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
            ByteBuf wholeMessageBuf = Unpooled.copiedBuffer(message,0,len);
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
                byte [] toSend = new byte [byteBuf.readableBytes()];
                byteBuf.readBytes(toSend);
                byteBuf.release();
                sendMessageAux(toSend,peer,messageId);
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
        DatagramPacket datagramPacket = new DatagramPacket(buf,peer);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(future.isSuccess()){
                scheduleRetransmission(all,messageId,peer,0);
                if(stats!=null){
                    stats.addSentBytes(peer,all.length,NetworkStatsKindEnum.MESSAGE_STATS);
                    stats.addSentBytes(peer,all.length, NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED);
                }
            }
            consumer.messageSentHandler(future.isSuccess(),future.cause(),null /*TODO message */,peer);
        });
    }
}
