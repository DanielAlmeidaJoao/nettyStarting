package org.streamingAPI.server.channelHandlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.server.listeners.InChannelListener;

//@ChannelHandler.Sharable
public class CustomHandshakeHandler extends ChannelHandlerAdapter {

    public static final String NAME ="CHSHAKE_HANDLER";
    private static final int UNCHANGED_VALUE = -2;

    private InChannelListener inChannelListener;
    private byte [] controlData;
    private int len;
    public CustomHandshakeHandler(InChannelListener inChannelListener){
       this.inChannelListener = inChannelListener;
       this.len = UNCHANGED_VALUE;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        if(in.readableBytes()<4){
            return;
        }
        if(len==UNCHANGED_VALUE){
            len = in.readInt();
        }
        if (len > 0 ){
            if(in.readableBytes()<len){
                return;
            }
            controlData = new byte[len];
            in.readBytes(controlData,0,len);
            inChannelListener.onControlDataRead(ctx.channel().id().asShortText(),controlData);
        }
        ctx.fireChannelRead(msg);
        ctx.channel().pipeline().remove(CustomHandshakeHandler.NAME);
    }
}
