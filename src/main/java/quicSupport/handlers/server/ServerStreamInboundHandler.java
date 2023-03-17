package quicSupport.handlers.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.funcHandlers.StreamListenerExecutor;

public class ServerStreamInboundHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);

    private InNettyChannelListener listener;
    private final StreamListenerExecutor streamListenerExecutor;

    public ServerStreamInboundHandler(InNettyChannelListener listener, StreamListenerExecutor streamListenerExecutor) {
        this.listener = listener;
        this.streamListenerExecutor = streamListenerExecutor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        streamListenerExecutor.onStreamCreated(ch);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        streamListenerExecutor.onStreamClosed(ch);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        ByteBuf byteBuf = (ByteBuf) msg;
        try {
            byte [] data = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(data);
            listener.onChannelRead(ch.id().asShortText(),data);
        } finally {
            byteBuf.release();
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        cause.printStackTrace();
        streamListenerExecutor.onStreamError(ch,cause);
    }
}
