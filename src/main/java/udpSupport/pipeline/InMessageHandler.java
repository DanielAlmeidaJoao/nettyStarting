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
        DatagramPacket datagramPacket = (DatagramPacket) o;
        ByteBuf content = datagramPacket.content();
        byte msgCode = content.readByte();
        logger.info("RECEIVED MESSAGE CODE: {}",(msgCode==UDPLogics.APP_ACK?"APP_MESSAGE":"ACK"));
        byte [] message=null;
        if(content.readableBytes()>0){
            message = new byte[content.readableBytes()];
        }
        Channel channel = channelHandlerContext.channel();
        switch (msgCode){
            case UDPLogics.APP_MESSAGE: onAppMessage(channel,message);break;
            case UDPLogics.APP_ACK: onAckMessage(channel);break;
            default: throw new RuntimeException("UNKNOWN MESSAGE CODE: "+msgCode);
        }

    }
    private void onAppMessage(Channel channel, byte [] message){
        InetSocketAddress dest = (InetSocketAddress) channel.remoteAddress();
        DatagramPacket datagramPacket = new DatagramPacket(Unpooled.buffer(1).writeByte(UDPLogics.APP_ACK),dest);
        channel.writeAndFlush(datagramPacket);
        if(channelStats!=null){
            channelStats.addReceivedBytes(dest,message.length+1,UDPLogics.APP_MESSAGE);
        }
        consumer.deliver(message,dest);
    }
    private void onAckMessage(Channel channel){
        InetSocketAddress dest = (InetSocketAddress) channel.remoteAddress();
        if(channelStats!=null){
            channelStats.addReceivedBytes(dest,1,UDPLogics.APP_ACK);
        }
    }
}

