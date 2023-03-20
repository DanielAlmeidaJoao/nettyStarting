package quicSupport.handlers.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;

public class ServerStreamInboundHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);

    private final QuicListenerExecutor streamListenerExecutor;

    public ServerStreamInboundHandler(QuicListenerExecutor streamListenerExecutor) {
        this.streamListenerExecutor = streamListenerExecutor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        streamListenerExecutor.onStreamCreated(ch);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("CHANNEL INACTIVE CALLED!!!");
        /**
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        String parentId = ctx.channel().parent().id().asShortText();
        String streamId = ctx.channel().id().asShortText();
        streamListenerExecutor.onChannelInactive(parentId,streamId);**/
        streamListenerExecutor.onStreamClosed((QuicStreamChannel) ctx.channel());
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        streamListenerExecutor.onChannelRead(ch.id().asShortText(),(byte []) msg);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        cause.printStackTrace();
        streamListenerExecutor.onStreamError(ch,cause);
    }
}
