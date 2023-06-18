package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.ConnectionId;
import quicSupport.utils.metrics.QuicChannelMetrics;

public class QuicServerChannelConHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "QuicServerChannelConHandler";
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerChannelConHandler.class);
    private ConnectionId id;

    public QuicServerChannelConHandler(CustomQuicChannelConsumer consumer, QuicChannelMetrics metrics) {
        this.consumer = consumer;
        this.metrics = metrics;
        id = null;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
    }
    public void setId(ConnectionId identification){
        assert id == null;
        id = identification;
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.onConnectionClosed(ctx.channel().remoteAddress());
        }
        consumer.channelInactive(id);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    }

    /*
    @Override
    public boolean isSharable() {
        return true;
    }

     */
}
