package udpSupport.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

public class UdpClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DatagramPacket) {
            DatagramPacket packet = (DatagramPacket) msg;
            ByteBuf data = packet.content();
            byte [] str = new byte[data.readableBytes()];
            data.readBytes(str);
            String got = new String(str);
            System.out.println("GOT - "+got);
            // Handle incoming packet data here
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Handle exception here
        cause.printStackTrace();
        ctx.close();
    }
}
