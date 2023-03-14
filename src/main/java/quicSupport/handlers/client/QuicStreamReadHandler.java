package quicSupport.handlers.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.CharsetUtil;

public class QuicStreamReadHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("READING");
        ByteBuf byteBuf = (ByteBuf) msg;
        System.out.println(byteBuf.toString(CharsetUtil.US_ASCII));
        byteBuf.release();
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
}
