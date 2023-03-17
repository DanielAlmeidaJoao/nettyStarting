package quicSupport.handlers.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.client_server.QuicClientExample;
import quicSupport.handlers.funcHandlers.StreamListenerExecutor;

public class QuicStreamReadHandler extends ChannelInboundHandlerAdapter {

    private InNettyChannelListener listener;
    private final StreamListenerExecutor streamListenerExecutor;

    public QuicStreamReadHandler(InNettyChannelListener listener, StreamListenerExecutor streamListenerExecutor) {
        this.listener = listener;
        this.streamListenerExecutor = streamListenerExecutor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        streamListenerExecutor.onStreamCreated((QuicStreamChannel) ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        streamListenerExecutor.onStreamClosed((QuicStreamChannel) ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        byte [] data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(data);
        byteBuf.release();
        listener.onChannelRead(ctx.channel().id().asShortText(),data);
    }

    /**
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt == ChannelInputShutdownReadComplete.INSTANCE) {
            // Close the connection once the remote peer did send the FIN for this stream.
            ((QuicChannel) ctx.channel().parent()).close(true, 0,
                    ctx.alloc().directBuffer(16)
                            .writeBytes(new byte[]{'k', 't', 'h', 'x', 'b', 'y', 'e'}));
        }
    }**/
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        streamListenerExecutor.onStreamError((QuicStreamChannel) ctx.channel(),cause);
    }
}
