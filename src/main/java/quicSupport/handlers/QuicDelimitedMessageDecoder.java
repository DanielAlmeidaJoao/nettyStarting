package quicSupport.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.server.channelHandlers.encodings.DelimitedMessageDecoder;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.utils.Logic;

import java.util.List;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
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
        StreamMessageEncapsulator streamMessageEncapsulator = new StreamMessageEncapsulator(msgType,data);
        out.add(streamMessageEncapsulator);
    }
    private void beforeLikeThis(){
        /**
        ByteBuf msgData = in.readSlice(in.readableBytes());

        StreamMessage streamMessage = new StreamMessage(msgData.array(),msgData.readableBytes(),ctx.channel().id().asShortText());
        out.add(streamMessage);
         **/
    }
}
