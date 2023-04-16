package udpSupport.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import udpSupport.channels.UDPChannelConsumer;

import java.nio.charset.StandardCharsets;

public class ClientHandler extends SimpleChannelInboundHandler {
    private UDPChannelConsumer consumer;

    public ClientHandler(UDPChannelConsumer consumer){
        this.consumer = consumer;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        DatagramPacket datagramPacket = (DatagramPacket) o;
        ByteBuf content = datagramPacket.content();
        byte [] message = new byte[content.readableBytes()];
        content.readBytes(message);
        System.out.println("CLIENT RECEIVED "+message.length);
    }
}
