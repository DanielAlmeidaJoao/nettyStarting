package org.streamingAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.streamingAPI.metrics.TCPStreamConnectionMetrics;
import org.streamingAPI.metrics.TCPStreamMetrics;

import java.util.List;

public class DelimitedMessageEncoder extends MessageToByteEncoder {

    public DelimitedMessageEncoder() {}

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf){
        ByteBuf buf = (ByteBuf) o;
        byteBuf.writeBytes(buf);
        /**
        buf.markReaderIndex();
        int len = buf.readInt();
        if(len+4 != buf.readableBytes()){
            /*
             * ByteBuf b = ...
             * b.writeInt(data.len)
             * b.writeBytes(data)
             */
            //throw new RuntimeException("THE FIRST 4 BYTES MUST BE THE LENGTH OF THE DATA TO BE SENT!");
            //buf.resetReaderIndex();
            //byteBuf.writeInt(buf.readableBytes());
        //}
        //byteBuf.writeBytes(buf,0, buf.readableBytes());**/
    }
}
