package org.streamingAPI.server.channelHandlers;

import babel.appExamples.channels.messages.StreamMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.streamingAPI.server.listeners.InChannelListener;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {

    private static final int UNCHANGED_VALUE = -2;


    public StreamReceiverHandler(InChannelListener inChannelListener){
        super(inChannelListener);
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        StreamMessage streamMessage = (StreamMessage) msg;
        getInChannelListener().onChannelReadWithByteBuf(ctx.channel().id().asShortText(),streamMessage);
    }
}
