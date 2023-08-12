package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.InputStream;

public class InputStreamEncoder  extends MessageToByteEncoder<InputStream> {
    @Override
    protected void encode(ChannelHandlerContext ctx, InputStream msg, ByteBuf out) throws Exception {

    }
}
