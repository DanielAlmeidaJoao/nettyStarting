package org.example.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.example.handlerFunctions.receiver.StreamReceiverChannelActiveFunction;
import org.example.handlerFunctions.receiver.StreamReceiverEOSFunction;
import org.example.handlerFunctions.receiver.StreamReceiverFirstBytesHandler;
import org.example.handlerFunctions.receiver.StreamReceiverFunction;

import java.util.concurrent.atomic.AtomicLong;

//@ChannelHandler.Sharable
public class CustomHandshakeHandler extends ChannelHandlerAdapter {

    public static final String NAME ="CHSHAKE_HANDLER";

    private ByteBuf tmp;

    StreamReceiverFirstBytesHandler firstBytesHandler;
    public CustomHandshakeHandler(StreamReceiverFirstBytesHandler firstBytesHandler){
       this.firstBytesHandler = firstBytesHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        tmp = ctx.alloc().buffer();
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("HANDSHAKE HANDLER!!!");
        ByteBuf in = (ByteBuf) msg;
        tmp.writeBytes(in);
        ctx.channel().pipeline().remove(CustomHandshakeHandler.NAME);
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        byte [] data = new byte[tmp.readableBytes()];
        tmp.readBytes(data);
        firstBytesHandler.execute(data);
        tmp.release();
        tmp=null;
        ctx.channel().pipeline().remove(CustomHandshakeHandler.NAME);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
