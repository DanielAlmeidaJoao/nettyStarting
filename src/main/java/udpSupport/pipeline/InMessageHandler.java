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
import udpSupport.client_server.NettyUDPServer;
import udpSupport.metrics.ChannelStats;
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

    public InMessageHandler(UDPChannelConsumer consumer, ChannelStats channelStats){
        this.consumer = consumer;
        this.channelStats = channelStats;
        receivedMessages = new ConcurrentSkipListSet<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().eventLoop().schedule(() -> {
            int len = receivedMessages.size();
            receivedMessages.clear();
            System.out.println("RECEIVED IDS CLEARED - "+len);
        },1, TimeUnit.MINUTES);
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
        }
        content.release();
        Channel channel = channelHandlerContext.channel();
        switch (msgCode){
            case UDPLogics.APP_MESSAGE: onAppMessage(channel,msgId,message,datagramPacket.sender());break;
            case UDPLogics.APP_ACK: onAckMessage(msgId,datagramPacket.sender());break;
            default: System.out.println("DEFAULT ");throw new Exception("UNKNOWN MESSAGE CODE: "+msgCode);
        }

    }
    private void onAppMessage(Channel channel,long msgId, byte [] message, InetSocketAddress sender){
        if(!receivedMessages.add(msgId)){
            logger.info("RECEIVED REPEATED MSG ID: ",msgId);
            return;
        }
        ByteBuf buf = Unpooled.buffer(9);
        buf.writeByte(UDPLogics.APP_ACK);
        buf.writeLong(msgId);
        DatagramPacket datagramPacket = new DatagramPacket(buf,sender);
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(!future.isSuccess()){
                future.cause().printStackTrace();
            }
        });
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,message.length+9,UDPLogics.APP_MESSAGE);
        }
        consumer.deliver(message,sender);
    }
    private void onAckMessage(long msgId, InetSocketAddress sender){
        consumer.deliverAck(msgId);
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,9,UDPLogics.APP_ACK);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}

