package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.QuicNettyImplementations.CustomQuicChannelConsumer;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.client_server.QUICServerEntity;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.customConnections.CustomQUICStreamCon;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.NewChannelsFactoryUtils;

public class QuicStreamInboundHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QUICServerEntity.class);
    public static final String HANDLER_NAME="QuicStreamInboundHandler";
    private final String customId;
    private final boolean inConnection;
    private CustomQUICStreamCon streamCon;

    private final CustomQuicChannelConsumer consumer;

    public QuicStreamInboundHandler(CustomQuicChannelConsumer streamListenerExecutor, String id, boolean incommingCon) {
        this.consumer = streamListenerExecutor;
        this.customId = id;
        this.inConnection = incommingCon;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelPipeline cp = ctx.channel().pipeline();
        //cp.addLast(QuicStructuredMessageEncoder.HANDLER_NAME,new QuicStructuredMessageEncoder());
        cp.addLast(QuicDelimitedMessageDecoder.HANDLER_NAME,new QuicDelimitedMessageDecoder(consumer,inConnection,customId));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        consumer.streamInactiveHandler((QuicStreamChannel) ctx.channel(),customId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        logger.error(cause.getMessage());
        consumer.streamErrorHandler((QuicStreamChannel) ctx.channel(),cause,customId);
        NewChannelsFactoryUtils.closeOnError(ctx.channel());

    }
}
