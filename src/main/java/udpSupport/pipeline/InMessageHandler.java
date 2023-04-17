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
import udpSupport.utils.funcs.OnAckFunction;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class InMessageHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(InMessageHandler.class);
    private Set<Long> receivedMessages;

    private final UDPChannelConsumer consumer;
    private final ChannelStats channelStats;
    private final OnAckFunction onAckfunction;

    public InMessageHandler(UDPChannelConsumer consumer, ChannelStats channelStats, OnAckFunction onAckfunction){
        this.consumer = consumer;
        this.channelStats = channelStats;
        this.onAckfunction = onAckfunction;
        receivedMessages = new ConcurrentSkipListSet<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().eventLoop().schedule(() -> {
            System.out.println("MISPLACED "+misplaced);
            int len = receivedMessages.size();
            receivedMessages.clear();
            System.out.println("RECEIVED IDS CLEARED - "+len);
        },10, TimeUnit.MINUTES);
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
            case UDPLogics.APP_MESSAGE: onAppMessage(channel,msgId,message,datagramPacket.sender());break;
            case UDPLogics.APP_ACK: onAckMessage(msgId,datagramPacket.sender());break;
            default: System.out.println("DEFAULT ");throw new Exception("UNKNOWN MESSAGE CODE: "+msgCode);
        }

    }
    long previous = -1;
    int misplaced = 0;
    private void onAppMessage(Channel channel,long msgId, byte [] message, InetSocketAddress sender){
        ByteBuf buf = Unpooled.buffer(9);
        buf.writeByte(UDPLogics.APP_ACK);
        buf.writeLong(msgId);
        DatagramPacket datagramPacket = new DatagramPacket(buf,sender);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(future.isSuccess()){
                if(channelStats!=null){
                    channelStats.addSentBytes(sender,9,UDPLogics.APP_ACK);
                }
            }else{
                future.cause().printStackTrace();
            }
        });

        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,message.length+9,UDPLogics.APP_MESSAGE);
        }
        if(!receivedMessages.add(msgId)){
            logger.info("RECEIVED REPEATED MSG ID: ",msgId);
            //System.out.println("RECEIVED REPEATED MSG ID: "+msgId);
            return;
        }
        if(previous<msgId){
            previous = msgId;
        }else{
            misplaced++;
        }
        consumer.deliverMessage(message,sender);
    }
    private void onAckMessage(long msgId, InetSocketAddress sender){
        onAckfunction.execute(msgId);
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,9,UDPLogics.APP_ACK);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}

