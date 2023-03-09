package org.streamingAPI.server.channelHandlers;

import babel.appExamples.channels.messages.StreamMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        ByteBuf msgData = in.readSlice(in.readableBytes());

        StreamMessage streamMessage = new StreamMessage(msgData.array(),msgData.readableBytes(),ctx.channel().id().asShortText());
        out.add(streamMessage);

    }
}
