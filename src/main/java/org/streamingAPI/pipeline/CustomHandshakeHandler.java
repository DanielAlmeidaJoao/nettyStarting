package org.streamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.streamingAPI.client.StreamOutConnection;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.utils.FactoryMethods;

//@ChannelHandler.Sharable
public class CustomHandshakeHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME ="CHSHAKE_HANDLER";
    private static final int UNCHANGED_VALUE = -2;

    private InNettyChannelListener inNettyChannelListener;
    private byte [] controlData;
    private int len;
    public CustomHandshakeHandler(InNettyChannelListener inNettyChannelListener){
       this.inNettyChannelListener = inNettyChannelListener;
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
            String gg = new String(controlData);
            HandShakeMessage handShakeMessage = FactoryMethods.g.fromJson(gg, HandShakeMessage.class);
            inNettyChannelListener.onChannelActive(ctx.channel(),handShakeMessage);
        }
        ctx.fireChannelRead(msg);
        ctx.channel().pipeline().remove(CustomHandshakeHandler.NAME);
    }
}
