package quicSupport.handlers.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;

public class QuicStreamReadHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';
    private final QuicListenerExecutor streamListenerExecutor;

    public QuicStreamReadHandler(QuicListenerExecutor streamListenerExecutor) {
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
        streamListenerExecutor.onStreamClosed((QuicStreamChannel) ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        /**
        StreamMessageEncapsulator data = (StreamMessageEncapsulator) msg;
        if(QuicDelimitedMessageDecoder.APP_DATA==data.getMsgType()){
            QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
            streamListenerExecutor.onChannelRead(ch.id().asShortText(),(data.getData()));
        }else{
            HandShakeMessage shakeMessage = Logic.gson.fromJson(new String(data.getData()),HandShakeMessage.class);
            streamListenerExecutor.onChannelActive((QuicStreamChannel) ctx.channel(),shakeMessage,true);
        }**/
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        cause.printStackTrace();
        streamListenerExecutor.onStreamError(ch,cause);
    }
}
