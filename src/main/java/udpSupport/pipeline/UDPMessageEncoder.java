package udpSupport.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import java.util.Map;

public class UDPMessageEncoder extends MessageToMessageEncoder<DatagramPacket> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, List<Object> list) throws Exception {
        channelHandlerContext.writeAndFlush(datagramPacket);
    }
}
