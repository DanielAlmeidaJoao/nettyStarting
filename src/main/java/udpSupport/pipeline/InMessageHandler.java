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
import udpSupport.utils.funcs.OnAckFunction;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class InMessageHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(InMessageHandler.class);
    private final Map<Long, SortedMap<Long,byte []>> streams;
    private final Set<Long> receivedMessages;

    private final UDPChannelConsumer consumer;
    private final ChannelStats channelStats;
    private final OnAckFunction onAckfunction;

    public InMessageHandler(UDPChannelConsumer consumer, ChannelStats channelStats, OnAckFunction onAckfunction){
        this.consumer = consumer;
        this.channelStats = channelStats;
        this.onAckfunction = onAckfunction;
        receivedMessages = new ConcurrentSkipListSet<>();
        streams = new HashMap<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().eventLoop().schedule(() -> {
            receivedMessages.clear();
        },2, TimeUnit.MINUTES);
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        DatagramPacket datagramPacket = (DatagramPacket) o;
        ByteBuf content = datagramPacket.content();
        byte msgCode = content.readByte();
        long msgId = content.readLong();
        //logger.info("RECEIVED MESSAGE CODE: {}",(msgCode==UDPLogics.APP_ACK?"APP_MESSAGE":"ACK"));
        byte [] message = null;
        Channel channel = channelHandlerContext.channel();

        if(UDPLogics.STREAM_MESSAGE==msgCode){
            long streamId = content.readLong();
            int streamCount = content.readInt();
            message = new byte[content.readableBytes()];
            content.readBytes(message);
            content.release();
            onStreamRead(channel,streamId,message,streamCount,datagramPacket.sender(),msgId);
        }else if(UDPLogics.SINGLE_MESSAGE==msgCode){
            message = new byte[content.readableBytes()];
            content.readBytes(message);
            content.release();
            onSingleMessage(channel,msgId,message,datagramPacket.sender());
        }else if ( UDPLogics.APP_ACK==msgCode){
            content.release();
            onAckMessage(msgId,datagramPacket.sender());
        }else{
            content.release();
            throw new Exception("UNKNOWN MESSAGE CODE: "+msgCode);
        }
    }
    long count = 0;
    int msgs = 0;
    private void onStreamRead(Channel channel,long streamId, byte [] message , int streamCount,InetSocketAddress sender,long msgId){
        int receivedBytes = message.length+8+8+4+1;
        sendAck(channel, msgId, sender);
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,receivedBytes,NetworkStatsKindEnum.MESSAGE_STATS);
        }
        if(!receivedMessages.add(msgId)){
            return;
        }
        count += message.length;
        SortedMap<Long,byte []> compute = streams.get(streamId);
        if(compute == null){
            compute = new TreeMap<>();
            streams.put(streamId,compute);
        }
        compute.put(msgId,message);
        if(compute.size()==streamCount){
            ByteBuf all = Unpooled.buffer();
            for (byte[] bytes : compute.values()) {
                all.writeBytes(bytes);
            }
            message = new byte[all.readableBytes()];
            all.readBytes(message);
            all.release();
            streams.remove(streamId);
            consumer.deliverMessage(message,sender);
            msgs++;
        }
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,receivedBytes,NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED);
        }
    }
    private void onSingleMessage(Channel channel,long msgId, byte [] message, InetSocketAddress sender){
        sendAck(channel, msgId, sender);
        if(channelStats!=null){
            //TODO: add another parameter that indicates the length of the message received
            channelStats.addReceivedBytes(sender,message.length+9,NetworkStatsKindEnum.MESSAGE_STATS);
        }
        if(!receivedMessages.add(msgId)){
            //logger.info("RECEIVED REPEATED MSG ID: ",msgId);
            return;
        }
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,message.length+9,NetworkStatsKindEnum.EFFECTIVE_SENT_DELIVERED);
        }
        consumer.deliverMessage(message,sender);
    }

    private void sendAck(Channel channel, long msgId, InetSocketAddress sender) {
        ByteBuf buf = Unpooled.buffer(9);
        buf.writeByte(UDPLogics.APP_ACK);
        buf.writeLong(msgId);
        DatagramPacket datagramPacket = new DatagramPacket(buf, sender);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(future.isSuccess()){
                if(channelStats!=null){
                    channelStats.addSentBytes(sender,9, NetworkStatsKindEnum.ACK_STATS);
                }
            }else{
                future.cause().printStackTrace();
            }
        });
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

