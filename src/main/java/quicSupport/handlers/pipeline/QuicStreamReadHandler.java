package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.lang3.tuple.Triple;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.client_server.QuicServerExample;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

public class QuicStreamReadHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicServerExample.class);
    public static final String HANDLER_NAME="QuicStreamReadHandler";

    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;

    public QuicStreamReadHandler(CustomQuicChannelConsumer streamListenerExecutor, QuicChannelMetrics metrics) {
        this.consumer = streamListenerExecutor;
        this.metrics = metrics;
    }

    public void notifyAppDelimitedStreamCreated(QuicStreamChannel quicStreamChannel, TransmissionType type, Triple<Short,Short,Short> triple){
        if(metrics!=null){
            QuicConnectionMetrics m = metrics.getConnectionMetrics(quicStreamChannel.parent().remoteAddress());
            m.setStreamCount(m.getStreamCount()+1);
        }
        consumer.streamCreatedHandler(quicStreamChannel,type,triple);
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        consumer.streamClosedHandler((QuicStreamChannel) ctx.channel());
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        consumer.streamErrorHandler((QuicStreamChannel) ctx.channel(),cause);
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                // Handle idle timeout event
                System.out.println("Idle timeout has occurred.");
                //ctx.close();
            }
        }
    }
}
