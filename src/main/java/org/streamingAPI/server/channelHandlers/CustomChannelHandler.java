package org.streamingAPI.server.channelHandlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import org.streamingAPI.server.listeners.InChannelListener;

//@ChannelHandler.Sharable
public abstract class CustomChannelHandler extends ChannelHandlerAdapter {
    private int totalRead;
    @Getter
    private InChannelListener inChannelListener;
    private boolean deliverDelimited;

    public CustomChannelHandler(InChannelListener inChannelListener,boolean readDelimited){
        this.inChannelListener  = inChannelListener;
        this.deliverDelimited=readDelimited;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        inChannelListener.onChannelActive(ctx.channel().id().asShortText());
    }
    private void deliverRead(ByteBuf in, String streamId){
        try {
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            inChannelListener.onChannelRead(streamId,bytes);
            totalRead += bytes.length;
        }catch (Exception e ){
            e.printStackTrace();
        }finally {
            ReferenceCountUtil.release(in);
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        deliverRead(in,ctx.channel().id().asShortText());
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {}
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        inChannelListener.onChannelInactive(ctx.channel().id().asShortText());
        System.out.printf("CHANNEL %S CLOSED. TOTAL READ %S \n",ctx.channel().id().asShortText()+"",totalRead+"");
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
