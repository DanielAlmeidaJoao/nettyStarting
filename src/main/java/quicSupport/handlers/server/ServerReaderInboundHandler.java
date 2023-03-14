package quicSupport.handlers.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.QuicServerExample;

import java.nio.charset.StandardCharsets;

public class ServerReaderInboundHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        Channel channel = ctx.channel();
        System.out.println("RECEIVED FOR STREAM: "+channel.id().asShortText());
        ByteBuf byteBuf = (ByteBuf) msg;
        try {
            String got = byteBuf.toString(CharsetUtil.US_ASCII);
            System.out.println(" oo "+ got);
            ByteBuf buffer = ctx.alloc().directBuffer();
            buffer.writeBytes(got.getBytes(StandardCharsets.UTF_8));
            // Write the buffer and shutdown the output by writing a FIN.
            ctx.writeAndFlush(buffer); //.addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
        } finally {
            byteBuf.release();
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
