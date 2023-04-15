package udpSupport.client_server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.nio.charset.StandardCharsets;

public class ClientHandler extends SimpleChannelInboundHandler {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        DatagramPacket datagramPacket = (DatagramPacket) o;
        ByteBuf data = datagramPacket.content();
        String message = data.toString(StandardCharsets.UTF_8);
        System.out.println("RECEIVED: "+datagramPacket.recipient()+" "+message);
    }
}
