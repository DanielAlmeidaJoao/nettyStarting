package org.streamingAPI.server.channelHandlers;

import babel.appExamples.channels.messages.StreamMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.streamingAPI.server.listeners.InChannelListener;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends CustomChannelHandler {
    public StreamReceiverHandler(InChannelListener inChannelListener, boolean deliverDelimited ){
        super(inChannelListener,deliverDelimited);
    }
    /**
     * Used when the sender specified the length of the data in the first bytes of the array
     * @param ctx
     * @param msg
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        getInChannelListener().onChannelRead(ctx.channel().id().asShortText(), (byte []) msg);
    }
}
