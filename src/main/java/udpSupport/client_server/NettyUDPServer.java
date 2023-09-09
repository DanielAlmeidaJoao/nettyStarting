package udpSupport.client_server;

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tcpSupport.tcpChannelAPI.connectionSetups.TCPServerEntity;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.metrics.ChannelStats;
import udpSupport.metrics.NetworkStatsKindEnum;
import udpSupport.pipeline.InMessageHandler;
import udpSupport.utils.UDPLogics;
import udpSupport.utils.UDPWaitForAckWrapper;
import udpSupport.utils.funcs.OnAckFunction;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyUDPServer {
    public static SecureRandom randomInstance;
    private static final Logger logger = LogManager.getLogger(NettyUDPServer.class);
    public final int BUFFER_SIZE;
    public static final String MIN_UDP_RETRANSMISSION_TIMEOUT = "UDP_RETRANSMISSION_TIMEOUT";
    public static final String MAX_UDP_RETRANSMISSION_TIMEOUT = "MAX_UDP_RETRANSMISSION_TIMEOUT";

    public int RETRANSMISSION_TIMEOUT;
    private final int MAX_RETRANSMISSION_TIMEOUT;
    public final int MAX_SEND_RETRIES;
    public static final String MAX_SEND_RETRIES_KEY = "UPD_MAX_SEND_RETRIES";
    public static final String UDP_BROADCAST_PROP="broadcast";


    private Map<Long,UDPWaitForAckWrapper> waitingForAcks;
    private final AtomicLong datagramPacketCounter;
    private final AtomicLong streamIdCounter;

    private final Channel channel;
    private final UDPChannelConsumer consumer;
    private final InetSocketAddress address;
    private final ChannelStats stats;
    private final Properties properties;
    private final SecureRandom random;

    private final EventLoopGroup group;



    public NettyUDPServer(UDPChannelConsumer consumer, ChannelStats stats, InetSocketAddress address, Properties properties){
        this.properties=properties;
        this.stats = stats;
        this.consumer = consumer;
        this.address = address;
        waitingForAcks = new ConcurrentHashMap<>();
        datagramPacketCounter = new AtomicLong(0);
        streamIdCounter = new AtomicLong(0);
        MAX_SEND_RETRIES = (properties.getProperty(UDP_BROADCAST_PROP)!=null ? 0: ( Integer.parseInt(properties.getProperty(MAX_SEND_RETRIES_KEY,"20"))));
        RETRANSMISSION_TIMEOUT = Integer.parseInt(properties.getProperty(MIN_UDP_RETRANSMISSION_TIMEOUT,"250"));
        MAX_RETRANSMISSION_TIMEOUT = Integer.parseInt(properties.getProperty(MAX_UDP_RETRANSMISSION_TIMEOUT,"0"));
        BUFFER_SIZE = Integer.parseInt((String) properties.getOrDefault(TCPChannelUtils.BUFF_ALOC_SIZE,"66560"));
        random = RETRANSMISSION_TIMEOUT>0 ? getRandomInstance():null;
        int serverThreads = FactoryMethods.serverThreads(properties);
        group = TCPServerEntity.createNewWorkerGroup(serverThreads);
        try {
            channel = start();
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("UDP LISTENER COULD NOT START!");
        }
    }
    public EventLoop getLoop(){
        return group.next();
    }
    public void onAckReceived(long msgId, InetSocketAddress sender){
        UDPWaitForAckWrapper timeMillis = waitingForAcks.remove(msgId);
        if(stats!=null&&timeMillis!=null){
            timeMillis.scheduledFuture.cancel(true);
            stats.addTransmissionRTT(sender,(System.currentTimeMillis() - timeMillis.timeStart));
        }
    }

    private void scheduleRetransmission(ByteBuf packet, long msgId, InetSocketAddress dest, int count){
        //NO RETRANSMISSION
        if(MAX_SEND_RETRIES <= 0){
            return;
        }
        UDPWaitForAckWrapper udpWaitForAckWrapper = waitingForAcks.get(msgId);
        ScheduledFuture scheduledFuture = channel.eventLoop().schedule(() -> {
            if(!waitingForAcks.containsKey(msgId)) {
                packet.release();
                return;
            }
            if(count > MAX_SEND_RETRIES){
                packet.release();
                waitingForAcks.remove(msgId);
                consumer.peerDown(dest);
                return;
            }
            final ByteBuf copy = packet.retainedDuplicate();
            int len = packet.readableBytes();
            channel.writeAndFlush(new DatagramPacket(copy,dest)).addListener(future -> {
                if(future.isSuccess()){
                    if(stats!=null){
                        stats.addSentBytes(dest,len, NetworkStatsKindEnum.MESSAGE_STATS);
                    }
                    scheduleRetransmission(packet,msgId,dest,count+1);
                }else{
                    future.cause().printStackTrace();
                }
            });
        }, RETRANSMISSION_TIMEOUT +  (int)nextFloat() ,TimeUnit.MILLISECONDS);
        if(count==0){
            waitingForAcks.put(msgId,new UDPWaitForAckWrapper(scheduledFuture));
        }else if(udpWaitForAckWrapper==null){
            scheduledFuture.cancel(true);
        }else{
            udpWaitForAckWrapper.scheduledFuture=scheduledFuture;
        }
    }

    private float nextFloat(){
        if(MAX_RETRANSMISSION_TIMEOUT>0){
            float stella = 1 + random.nextInt(1+random.nextInt(MAX_RETRANSMISSION_TIMEOUT));
            if(random.nextBoolean()){
                stella += random.nextFloat()*stella/2;
            }else{
                stella -= random.nextFloat()*stella;
            }
            return stella;
        }else{
            return random.nextInt(1+RETRANSMISSION_TIMEOUT);
        }
    }
    public static Class<? extends Channel> socketChannel(){
        return Epoll.isAvailable() ? EpollDatagramChannel.class:NioDatagramChannel.class;
    }
    private Channel start() throws Exception{
        OnAckFunction onAckReceived = this::onAckReceived;
        Channel server;
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(socketChannel())
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(BUFFER_SIZE))
                .option(ChannelOption.SO_BROADCAST, properties.getProperty(UDP_BROADCAST_PROP)!=null)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new InMessageHandler(consumer,stats,onAckReceived,MAX_SEND_RETRIES));
                    }
                });
        server = b.bind(address).sync().channel();
        server.closeFuture().addListener(future -> {
            //System.out.println("UDP SERVER DOWN");
            group.shutdownGracefully().getNow();
            logger.debug("Server socket closed. " + (future.isSuccess() ? "" : "Cause: " + future.cause()));
        });
        logger.debug("UDP SERVER LISTENING ON : {}",address);
        return server;
    }
    public void sendMessage(ByteBuf message, InetSocketAddress peer){
        if(UDPLogics.MAX_UDP_PAYLOAD_SIZE<message.readableBytes()){
            message.readByte();
            message.readLong();
            long streamId = streamIdCounter.incrementAndGet();
            ByteBuf wholeMessageBuf = message; //Unpooled.wrappedBuffer(message);
            int streamCount = ceilDiv(message.readableBytes(),UDPLogics.MAX_UDP_PAYLOAD_SIZE); //do the %
            for (int i = 0; i < streamCount; i++) {
                long messageId = datagramPacketCounter.incrementAndGet();
                int streamLen = Math.min(wholeMessageBuf.readableBytes(), UDPLogics.MAX_UDP_PAYLOAD_SIZE);

                ByteBuf byteBuf = channel.alloc().directBuffer();/* Byte.BYTES+Long.BYTES*2+Integer.BYTES+streamLen */;
                byteBuf.writeByte(UDPLogics.STREAM_MESSAGE);
                byteBuf.writeLong(messageId);
                byteBuf.writeLong(streamId);
                byteBuf.writeInt(streamCount);
                byteBuf.writeBytes(wholeMessageBuf,streamLen);
                sendMessageAux(byteBuf,peer,messageId);
            }
            wholeMessageBuf.release();
        }else{
            //int len = message.readableBytes();
            long messageId = datagramPacketCounter.incrementAndGet();
            //ByteBuf buf = channel.alloc().directBuffer(9+len);
            message.setByte(0,UDPLogics.SINGLE_MESSAGE);
            message.setLong(1,messageId);
            //buf.writeBytes(message,0, len);
            sendMessageAux(message,peer,messageId);
        }
    }

    public ByteBuf alloc(){
        return channel.alloc().directBuffer();
    }
    private int ceilDiv(int a, int b){
        if(a%b==0){
            return a/b;
        }else{
            return (a/b) + 1;
        }
    }
    public void sendMessageAux(ByteBuf all, InetSocketAddress peer, long messageId){
        final int sent = all.readableBytes();
        channel.writeAndFlush(new DatagramPacket(all.retainedDuplicate(),peer));
        scheduleRetransmission(all,messageId,peer,0);
        if(stats!=null){
            stats.addSentBytes(peer,sent,NetworkStatsKindEnum.MESSAGE_STATS);
            stats.addSentBytes(peer,sent,NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED);
        }
    }
    public void shutDownServerClient(){
        channel.close();
        channel.disconnect();
    }
    private static SecureRandom getRandomInstance(){
        if(randomInstance==null){
            return new SecureRandom();
        }
        return randomInstance;
    }
}
