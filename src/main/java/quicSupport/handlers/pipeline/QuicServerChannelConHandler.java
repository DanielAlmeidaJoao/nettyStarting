package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.channels.CustomQuicChannelConsumer;
import tcpSupport.tcpChannelAPI.utils.NewChannelsFactoryUtils;

public class QuicServerChannelConHandler extends ChannelInboundHandlerAdapter {
    private final CustomQuicChannelConsumer consumer;
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerChannelConHandler.class);

    public QuicServerChannelConHandler(CustomQuicChannelConsumer consumer) {
        this.consumer = consumer;

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        consumer.channelInactive(ctx.channel().id().asShortText());
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(cause.getMessage());
        consumer.streamErrorHandler((QuicStreamChannel) ctx.channel(),cause,null);
        NewChannelsFactoryUtils.closeOnError(ctx.channel());
    }
}
