package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.channels.CustomQuicChannelConsumer;

public class QuicServerChannelConHandler extends ChannelInboundHandlerAdapter {
    private final CustomQuicChannelConsumer consumer;
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerChannelConHandler.class);

    public QuicServerChannelConHandler(CustomQuicChannelConsumer consumer) {
        this.consumer = consumer;

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        consumer.channelInactive(ctx.channel().id().asShortText());
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    }

    @Override
    public boolean isSharable() {
        return true;
    }
}
