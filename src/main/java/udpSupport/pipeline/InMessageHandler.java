package udpSupport.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannel;
import udpSupport.channels.UDPChannelConsumer;
import udpSupport.metrics.ChannelStats;
import udpSupport.utils.UDPLogics;

import java.net.InetSocketAddress;

public class InMessageHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(InMessageHandler.class);

    private final UDPChannelConsumer consumer;
    private final ChannelStats channelStats;

    public InMessageHandler(UDPChannelConsumer consumer, ChannelStats channelStats){
        this.consumer = consumer;
        this.channelStats = channelStats;
    }


    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        System.out.println("RECEIVING MESSAGE!!");
        DatagramPacket datagramPacket = (DatagramPacket) o;
        System.out.println("SENDER "+        datagramPacket.sender());
        ByteBuf content = datagramPacket.content();
        byte msgCode = content.readByte();
        long msgId = content.readLong();
        logger.info("RECEIVED MESSAGE CODE: {}",(msgCode==UDPLogics.APP_ACK?"APP_MESSAGE":"ACK"));
        System.out.println("RECEVED MESSAGE CODE"+msgCode+" "+msgId);
        byte [] message=null;
        if(content.readableBytes()>0){
            message = new byte[content.readableBytes()];
        }
        Channel channel = channelHandlerContext.channel();
        switch (msgCode){
            case UDPLogics.APP_MESSAGE: onAppMessage(channel,msgId,message,datagramPacket.sender());break;
            case UDPLogics.APP_ACK: onAckMessage(channel,msgId,datagramPacket.sender());break;
            default: System.out.println("DEFAULT ");throw new Exception("UNKNOWN MESSAGE CODE: "+msgCode);
        }

    }
    private void onAppMessage(Channel channel,long msgId, byte [] message, InetSocketAddress sender){
        System.out.println("APP MESSAGE ++++");
        ByteBuf buf = Unpooled.buffer(9);
        buf.writeByte(UDPLogics.APP_ACK);
        buf.writeLong(msgId);
        DatagramPacket datagramPacket = new DatagramPacket(buf,sender);
        System.out.println(" 33333333 ");
        channel.writeAndFlush(datagramPacket).addListener(future -> {
            if(future.isSuccess()){
                System.out.println(" ++++++++++++++++++++++++++++++++++ ");
            }else {
                future.cause().printStackTrace();
            }
        });
        System.out.println("ACK SENT");
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,message.length+1,UDPLogics.APP_MESSAGE);
        }
        consumer.deliver(message,sender);
    }
    private void onAckMessage(Channel channel, long msgId, InetSocketAddress sender){
        System.out.println("APP ACK");
        consumer.deliverAck(msgId);
        if(channelStats!=null){
            channelStats.addReceivedBytes(sender,1,UDPLogics.APP_ACK);
        }
    }
}

