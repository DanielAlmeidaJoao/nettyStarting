package org.streamingAPI.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.streamingAPI.handlerFunctions.receiver.ChannelActiveReadHandler;

//@ChannelHandler.Sharable
public class CustomHandshakeHandler extends ChannelHandlerAdapter {

    public static final String NAME ="CHSHAKE_HANDLER";

    private byte [] controlData;
    private ChannelActiveReadHandler firstBytesHandler;
    public CustomHandshakeHandler(ChannelActiveReadHandler firstBytesHandler){
       this.firstBytesHandler = firstBytesHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
    int cc = 0;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("HANDSHAKE HANDLER!!!");
        ByteBuf in = (ByteBuf) msg;
        if(in.readableBytes()<4){
            return;
        }
        int len = in.readInt();
        if (len > 0 ){
            cc++;
            System.out.println(len+" len -- cc "+cc);
            if(in.readableBytes()<len){
                return;
            }
            controlData = new byte[len];
            in.readBytes(controlData,0,len);
            firstBytesHandler.execute(controlData);
        }
        ctx.fireChannelRead(msg);
        ctx.channel().pipeline().remove(CustomHandshakeHandler.NAME);
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {

    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
