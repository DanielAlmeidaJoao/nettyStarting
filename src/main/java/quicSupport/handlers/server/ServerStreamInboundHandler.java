package quicSupport.handlers.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.server.channelHandlers.encodings.DelimitedMessageDecoder;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.QuicDelimitedMessageDecoder;
import quicSupport.handlers.StreamMessageEncapsulator;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logic;

public class ServerStreamInboundHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';
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

    public static ByteBuf writeBytes(int len, byte [] data, byte msgType){
        ByteBuf buf = Unpooled.buffer(len+5);
        buf.writeInt(len);
        buf.writeByte(msgType);
        buf.writeBytes(data,0,len);
        return buf;
    }
    public static void readBuff(QuicListenerExecutor streamListenerExecutor, ChannelHandlerContext ctx, Object msg){
        ByteBuf in = (ByteBuf) msg;

        if(in.readableBytes()<4){
            return;
        }
        in.markReaderIndex();
        int length = in.readInt();

        if(in.readableBytes()<length+1){
            in.resetReaderIndex();
            return;
        }
        byte msgType = in.readByte();
        byte [] data = new byte[length];
        in.readBytes(data);
        if(QuicDelimitedMessageDecoder.APP_DATA==msgType){
            QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
            streamListenerExecutor.onChannelRead(ch.id().asShortText(),(data));
        }else{
            HandShakeMessage shakeMessage = Logic.gson.fromJson(new String(data),HandShakeMessage.class);
            streamListenerExecutor.onChannelActive((QuicStreamChannel) ctx.channel(),shakeMessage,true);
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
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
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        cause.printStackTrace();
        streamListenerExecutor.onStreamError(ch,cause);
    }
}
