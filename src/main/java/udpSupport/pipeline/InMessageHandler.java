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
    private final Map<Long, SortedSet<byte []>> streams;
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
            int len = receivedMessages.size();
            receivedMessages.clear();
            System.out.println("RECEIVED IDS CLEARED - "+len);
        },2, TimeUnit.MINUTES);
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        DatagramPacket datagramPacket = (DatagramPacket) o;
        ByteBuf content = datagramPacket.content();
        byte msgCode = content.readByte();
        long msgId = content.readLong();
        logger.info("RECEIVED MESSAGE CODE: {}",(msgCode==UDPLogics.APP_ACK?"APP_MESSAGE":"ACK"));
        byte [] message=null;
        if(content.readableBytes()>0){
            message = new byte[content.readableBytes()];
            content.readBytes(message);
        }
        content.release();
        Channel channel = channelHandlerContext.channel();
        switch (msgCode){
            case UDPLogics.SINGLE_MESSAGE:/*TODO */break;
            case UDPLogics.STREAM_MESSAGE: /* */ break;
            case UDPLogics.APP_MESSAGE: onAppMessage(channel,msgId,message,datagramPacket.sender());break;
            case UDPLogics.APP_ACK: onAckMessage(msgId,datagramPacket.sender());break;
            default: System.out.println("DEFAULT ");throw new Exception("UNKNOWN MESSAGE CODE: "+msgCode);
        }
    }
    private byte [] readMessageBytes(ByteBuf buf){
        byte [] message = new byte[buf.readableBytes()];
        buf.readBytes(message);
        return message;
    }
    private void onStreamRead(ByteBuf buf){
        long streamId = buf.readLong();
        int streamCount = buf.readInt();
        byte [] message=readMessageBytes(buf);
        SortedSet<byte[]> compute = streams.compute(streamId, (aLong, bytes) -> new TreeSet<>());
        if(compute.size()==streamCount){
        }

    }
    private void onAppMessage(Channel channel,long msgId, byte [] message, InetSocketAddress sender){
        sendAck(channel, msgId, sender);
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,message.length+9,NetworkStatsKindEnum.MESSAGE_STATS);
        }
        if(!receivedMessages.add(msgId)){
            logger.info("RECEIVED REPEATED MSG ID: ",msgId);
            return;
        }
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,message.length+9,NetworkStatsKindEnum.MESSAGE_DELIVERED);
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
        onAckfunction.execute(msgId);
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,9,NetworkStatsKindEnum.ACK_STATS);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}

