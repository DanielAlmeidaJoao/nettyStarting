package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.client_server.QuicServerExample;
import quicSupport.utils.enums.TransmissionType;

public class QuicStreamReadHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicServerExample.class);
    public static final String HANDLER_NAME="QuicStreamReadHandler";

    private final CustomQuicChannelConsumer consumer;

    public QuicStreamReadHandler(CustomQuicChannelConsumer streamListenerExecutor) {
        this.consumer = streamListenerExecutor;
    }

    public void notifyAppDelimitedStreamCreated(QuicStreamChannel quicStreamChannel, TransmissionType type, String customId, boolean inConnection){
        consumer.streamCreatedHandler(quicStreamChannel,type,customId,inConnection);
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        consumer.streamInactiveHandler((QuicStreamChannel) ctx.channel());
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
