package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.handlers.nettyFuncHandlers.QuicListenerExecutor;
import quicSupport.utils.entities.QuicChannelMetrics;

public class QuicServerChannelConHandler extends ChannelInboundHandlerAdapter {
    private QuicListenerExecutor listener;
    private final QuicChannelMetrics metrics;
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerChannelConHandler.class);

    public QuicServerChannelConHandler(QuicListenerExecutor listener, QuicChannelMetrics metrics) {
        this.listener = listener;
        this.metrics = metrics;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(ctx.channel().config());
        LOGGER.info("SERVER CHANNEL ACTIVE!!!");
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.onConnectionClosed(ctx.channel().remoteAddress());
        }
        listener.onChannelInactive(ctx.channel().id().asShortText());
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
