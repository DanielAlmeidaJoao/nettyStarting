package udpSupport.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import udpSupport.metrics.ChannelStats;
import udpSupport.utils.MessageWrapper;
import udpSupport.utils.UDPLogics;

import java.util.List;

public class UDPMessageEncoder extends MessageToMessageEncoder<DatagramPacket> {
    private final ChannelStats channelStats;

    public UDPMessageEncoder(ChannelStats channelStats) {
        this.channelStats = channelStats;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, List<Object> list){
        /**
        if(channelStats!=null){
            ByteBuf buf = datagramPacket.content();
            buf.markReaderIndex();
            channelStats.addSentBytes(datagramPacket.sender(),buf.readableBytes(),buf.readByte());
            buf.resetReaderIndex();
        } **/
        System.out.println("COMING HEREEEEEEE");
        channelHandlerContext.writeAndFlush(datagramPacket);
    }
}
