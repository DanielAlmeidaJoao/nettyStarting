package quicSupport.handlers.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.client_server.QuicServerExample;

public class ServerStreamInboundHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);

    private InNettyChannelListener listener;

    public ServerStreamInboundHandler(InNettyChannelListener listener) {
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        System.out.println("VV STREAM CHANNEL ACTIVE "+ch.id().asShortText());
        System.out.println(ch.id().asLongText());
        System.out.println(ch.remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("STREAM READER");
        ByteBuf byteBuf = (ByteBuf) msg;
        try {
            String got = byteBuf.toString(CharsetUtil.US_ASCII);
            System.out.println(" oo "+ got);
            //ByteBuf buffer = ctx.alloc().directBuffer();
            //buffer.writeBytes(got.getBytes(StandardCharsets.UTF_8));
            // Write the buffer and shutdown the output by writing a FIN.
            //ctx.writeAndFlush(buffer); //.addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
        } finally {
            byteBuf.release();
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
