package quicSupport.handlers.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;

public class QuicStreamReadHandler extends ChannelInboundHandlerAdapter {

    private final QuicListenerExecutor streamListenerExecutor;

    public QuicStreamReadHandler(QuicListenerExecutor streamListenerExecutor) {
        this.streamListenerExecutor = streamListenerExecutor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        streamListenerExecutor.onStreamCreated((QuicStreamChannel) ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("client stream inactive lllcallled");
        /**
        String parentId = ctx.channel().parent().id().asShortText();
        String streamId = ctx.channel().id().asShortText();
        streamListenerExecutor.onChannelInactive(parentId,streamId);**/
        streamListenerExecutor.onStreamClosed((QuicStreamChannel) ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        byte [] data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(data);
        byteBuf.release();
        streamListenerExecutor.onChannelRead(ctx.channel().id().asShortText(),data);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        streamListenerExecutor.onStreamError((QuicStreamChannel) ctx.channel(),cause);
    }
}
