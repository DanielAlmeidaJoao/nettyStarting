package org.streamingAPI.server.channelHandlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import org.streamingAPI.server.listeners.InNettyChannelListener;

import java.net.InetSocketAddress;

//@ChannelHandler.Sharable
public abstract class CustomChannelHandler extends ChannelHandlerAdapter {
    private int totalRead;
    @Getter
    private InNettyChannelListener inNettyChannelListener;

    public CustomChannelHandler(InNettyChannelListener inNettyChannelListener){
        this.inNettyChannelListener = inNettyChannelListener;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        /** NULL because we do not care about this field on the server handler channelActive
        Channel channel = ctx.channel(); // get the channel from somewhere
        inNettyChannelListener.onChannelActive(ctx.channel(),null);
            **/
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        getInNettyChannelListener().onChannelRead(ctx.channel().id().asShortText(), (byte []) msg);
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {}
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        inNettyChannelListener.onChannelInactive(ctx.channel().id().asShortText());
        System.out.printf("CHANNEL %S CLOSED. TOTAL READ %S \n",ctx.channel().id().asShortText()+"",totalRead+"");
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
