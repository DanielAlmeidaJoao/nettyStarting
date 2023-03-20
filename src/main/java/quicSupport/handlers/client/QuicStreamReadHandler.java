package quicSupport.handlers.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.server.channelHandlers.encodings.DelimitedMessageDecoder;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.handlers.QuicDelimitedMessageDecoder;
import quicSupport.handlers.StreamMessageEncapsulator;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logic;


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
        streamListenerExecutor.onStreamClosed((QuicStreamChannel) ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //streamListenerExecutor.onChannelRead(ctx.channel().id().asShortText(),(byte []) msg);
        StreamMessageEncapsulator data = (StreamMessageEncapsulator) msg;
        if(QuicDelimitedMessageDecoder.APP_DATA==data.getMsgType()){
            QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
            streamListenerExecutor.onChannelRead(ch.id().asShortText(),(data.getData()));
        }else{
            HandShakeMessage shakeMessage = Logic.gson.fromJson(new String(data.getData()),HandShakeMessage.class);
            streamListenerExecutor.onChannelActive((QuicStreamChannel) ctx.channel(),shakeMessage,true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        streamListenerExecutor.onStreamError((QuicStreamChannel) ctx.channel(),cause);
    }
}
