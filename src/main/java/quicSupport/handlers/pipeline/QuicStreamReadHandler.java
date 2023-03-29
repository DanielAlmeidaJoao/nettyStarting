package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamChannelConfig;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.client_server.QuicServerExample;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

public class QuicStreamReadHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicServerExample.class);
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;

    public QuicStreamReadHandler(CustomQuicChannelConsumer streamListenerExecutor, QuicChannelMetrics metrics) {
        this.consumer = streamListenerExecutor;
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        QuicStreamChannelConfig config = ch.config();
        config.setAllowHalfClosure(false);
        if(metrics!=null){
            QuicConnectionMetrics m = metrics.getConnectionMetrics(ch.parent().remoteAddress());
            m.setStreamCount(m.getStreamCount()+1);
        }
        consumer.streamCreatedHandler(ch);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        consumer.streamClosedHandler((QuicStreamChannel) ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        cause.printStackTrace();
        consumer.streamErrorHandler(ch,cause);
    }
}
