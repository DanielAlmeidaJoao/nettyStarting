package udpSupport.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.metrics.ChannelStats;
import udpSupport.metrics.NetworkStatsKindEnum;
import udpSupport.utils.UDPLogics;
import udpSupport.utils.funcs.OnAckFunction;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class InMessageHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(InMessageHandler.class);
    private final Map<String, SortedMap<Long,byte []>> streams;
    private Map<String,Long> receivedMessages;

    private final UDPChannelConsumer consumer;
    private final ChannelStats channelStats;
    private final OnAckFunction onAckfunction;
    private final int timeoutDeleteReceivedIds;
    private final int maxSendRetry;
    private final boolean checkDuplicate;

    public InMessageHandler(UDPChannelConsumer consumer, ChannelStats channelStats, OnAckFunction onAckfunction, int maxSendRetry, int timeoutDeleteReceivedIds){
        this.consumer = consumer;
        this.channelStats = channelStats;
        this.onAckfunction = onAckfunction;
        this.timeoutDeleteReceivedIds = timeoutDeleteReceivedIds > 0 ? timeoutDeleteReceivedIds : (120*1000);
        streams = new HashMap<>();
        this.maxSendRetry = maxSendRetry;
        checkDuplicate = this.timeoutDeleteReceivedIds > 0 && this.maxSendRetry>0;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(canCheckDuplicate()){
            receivedMessages = new HashMap<>();
            ctx.channel().eventLoop().schedule(() -> {
                receivedMessages.clear();
            },timeoutDeleteReceivedIds,TimeUnit.SECONDS);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        DatagramPacket datagramPacket = (DatagramPacket) o;
        ByteBuf content = datagramPacket.content();
        final int availableBytes = content.readableBytes();
        byte msgCode = content.readByte();
        long msgId = content.readLong();
        //logger.info("RECEIVED MESSAGE CODE: {}",(msgCode==UDPLogics.APP_ACK?"APP_MESSAGE":"ACK"));
        Channel channel = channelHandlerContext.channel();

        if(UDPLogics.STREAM_MESSAGE==msgCode){
            sendAck(channel, msgId,datagramPacket.sender());
            long streamId = content.readLong();
            int streamCount = content.readInt();
            Long peerID = content.readLong();
            byte [] message = new byte[content.readableBytes()];
            content.readBytes(message);
            content.release();
            onStreamRead(peerID,channel,streamId,message,streamCount,datagramPacket.sender(),msgId,availableBytes);
        }else if(UDPLogics.SINGLE_MESSAGE==msgCode){
            sendAck(channel,msgId,datagramPacket.sender());
            //message = new byte[content.readableBytes()];
            //content.readBytes(message);
            //content.release();
            Long peerID = content.readLong();
            onSingleMessage(peerID,msgId,content,datagramPacket.sender(),availableBytes);
            content.release();
        }else if ( UDPLogics.APP_ACK==msgCode){
            content.release();
            onAckMessage(msgId,datagramPacket.sender());
        }else{
            content.release();
            new Exception("UNKNOWN MESSAGE CODE: "+msgCode).printStackTrace();
            //System.exit(1);
        }
    }
    private boolean canCheckDuplicate(){
        return checkDuplicate;
    }
    //long count = 0;
    //int msgs = 0;
    private String getStrID(InetSocketAddress sender,long msgId){
        return sender+""+msgId;
    }
    private void onStreamRead(Long peerID, Channel channel, long streamId, byte [] message , int streamCount, InetSocketAddress sender, long msgId, int availableBytes){
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,availableBytes,NetworkStatsKindEnum.MESSAGE_STATS);
        }
        if(canCheckDuplicate()){
            Long pID = receivedMessages.put(getStrID(sender,msgId),peerID);
            if(pID==peerID){
                return;
            }
        }
        //count += message.length;
        String strStreamId = getStrID(sender,streamId);
        SortedMap<Long,byte []> compute = streams.get(strStreamId);
        if(compute == null){
            compute = new TreeMap<>();
            streams.put(strStreamId,compute);
            channel.eventLoop().schedule(() -> {
                streams.remove(strStreamId);
            },1*streamCount, TimeUnit.MINUTES);
        }
        compute.put(msgId,message);
        if(compute.size()==streamCount){
            ByteBuf all = Unpooled.buffer();
            for (byte[] bytes : compute.values()) {
                all.writeBytes(bytes);
            }
            //message = new byte[all.readableBytes()];
            //all.readBytes(message);
            //all.release();
            streams.remove(strStreamId);
            consumer.deliverMessage(all,sender);
            all.release();
            //msgs++;
        }
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,availableBytes,NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED);
        }
    }
    private void onSingleMessage(Long peerID, long msgId, ByteBuf message, InetSocketAddress sender, int availableBytes){
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,availableBytes,NetworkStatsKindEnum.MESSAGE_STATS);
        }
        if(canCheckDuplicate()){
            Long pID = receivedMessages.put(getStrID(sender,msgId),peerID);
            if(pID==peerID){
                return;
            }
        }
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,availableBytes,NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED);
        }
        consumer.deliverMessage(message,sender);
    }

    private void sendAck(Channel channel, long msgId, InetSocketAddress sender) {
        if(maxSendRetry>0){
            ByteBuf buf = channel.alloc().buffer(9);
            buf.writeByte(UDPLogics.APP_ACK);
            buf.writeLong(msgId);
            DatagramPacket datagramPacket = new DatagramPacket(buf, sender);
            channel.writeAndFlush(datagramPacket);
            if(channelStats!=null){
                channelStats.addSentBytes(sender,9, NetworkStatsKindEnum.ACK_STATS);
            }
        }
    }

    private void onAckMessage(long msgId, InetSocketAddress sender){
        onAckfunction.execute(msgId,sender);
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,9,NetworkStatsKindEnum.ACK_STATS);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}

