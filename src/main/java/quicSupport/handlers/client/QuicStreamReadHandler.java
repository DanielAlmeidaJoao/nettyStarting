package quicSupport.handlers.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.CharsetUtil;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;

public class QuicStreamReadHandler extends ChannelInboundHandlerAdapter {

    private InNettyChannelListener listener;

    public QuicStreamReadHandler(InNettyChannelListener listener) {
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("CHANNEL ACTIVE");
        Channel channel = ctx.channel();
        //listener.onChannelActive(channel,null);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("CHANNEL INACTIVE!!!");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("READING");
        ByteBuf byteBuf = (ByteBuf) msg;
        byte [] data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(data);
        byteBuf.release();
        listener.onChannelRead(ctx.channel().id().asShortText(),data);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt == ChannelInputShutdownReadComplete.INSTANCE) {
            // Close the connection once the remote peer did send the FIN for this stream.
            ((QuicChannel) ctx.channel().parent()).close(true, 0,
                    ctx.alloc().directBuffer(16)
                            .writeBytes(new byte[]{'k', 't', 'h', 'x', 'b', 'y', 'e'}));
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
