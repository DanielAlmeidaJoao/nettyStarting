package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.metrics.QuicChannelMetrics;

public class QuicServerChannelConHandler extends ChannelInboundHandlerAdapter {
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerChannelConHandler.class);

    public QuicServerChannelConHandler(CustomQuicChannelConsumer consumer, QuicChannelMetrics metrics) {
        this.consumer = consumer;
        this.metrics = metrics;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.onConnectionClosed(ctx.channel().remoteAddress());
        }
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
