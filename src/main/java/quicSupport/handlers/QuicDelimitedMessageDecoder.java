package quicSupport.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logics;

import java.util.List;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';
    private QuicListenerExecutor streamListenerExecutor;
    public QuicDelimitedMessageDecoder(QuicListenerExecutor streamListenerExecutor){
        this.streamListenerExecutor=streamListenerExecutor;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if(msg.readableBytes()<4){
            return;
        }
        msg.markReaderIndex();
        int length = msg.readInt();

        if(msg.readableBytes()<length+1){
            msg.resetReaderIndex();
            return;
        }
        byte msgType = msg.readByte();
        byte [] data = new byte[length];
        msg.readBytes(data);
        if(QuicDelimitedMessageDecoder.APP_DATA==msgType){
            QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
            streamListenerExecutor.onChannelRead(ch.id().asShortText(),data);
        }else{
            //ONLY THE SERVER RECEIVES HANDSHAKE
            HandShakeMessage shakeMessage = Logics.gson.fromJson(new String(data),HandShakeMessage.class);
            streamListenerExecutor.onChannelActive((QuicStreamChannel) ctx.channel(),shakeMessage,true);
        }
    }
}
