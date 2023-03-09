package org.streamingAPI.server.channelHandlers.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class DelimitedMessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes()<4){
            return;
        }
        in.markReaderIndex();
        int length = in.readInt();
        if(in.readableBytes()<length){
            in.resetReaderIndex();
            return;
        }
        byte [] data = new byte[length];
        in.readBytes(data);
        out.add(data);
    }
    private void beforeLikeThis(){
        /**
        ByteBuf msgData = in.readSlice(in.readableBytes());

        StreamMessage streamMessage = new StreamMessage(msgData.array(),msgData.readableBytes(),ctx.channel().id().asShortText());
        out.add(streamMessage);
         **/
    }
}
