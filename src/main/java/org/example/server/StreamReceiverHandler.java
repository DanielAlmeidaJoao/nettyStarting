package org.example.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.example.logics.StreamReceiverChannelActiveFunction;
import org.example.logics.StreamReceiverEOSFunction;
import org.example.logics.StreamReceiverFunction;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//@ChannelHandler.Sharable
public class StreamReceiverHandler extends ChannelHandlerAdapter {

    private long timeElapsed;
    private AtomicLong totalRead;
    private StreamReceiverChannelActiveFunction activeFunction;
    private StreamReceiverFunction functionToExecute;
    private StreamReceiverEOSFunction EOSFunction;
    private int timesReceived = 0;

    public StreamReceiverHandler(StreamReceiverChannelActiveFunction activeFunction,
                                 StreamReceiverFunction functionToExecute,
                                 StreamReceiverEOSFunction EOSFunction){
        this.activeFunction = activeFunction;
        this.functionToExecute = functionToExecute;
        this.EOSFunction = EOSFunction;
        totalRead = new AtomicLong(0);
        timeElapsed = 0;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        activeFunction.execute(ctx.name());
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            while (in.isReadable()) {
                long start = System.currentTimeMillis();
                byte[] bytes = new byte[in.readableBytes()];
                long end = System.currentTimeMillis();
                timeElapsed += (end-start);
                in.readBytes(bytes);
                functionToExecute.execute(ctx.name(),bytes);

                totalRead.set(totalRead.get() + bytes.length);
                timesReceived++;
            }
        }catch (Exception e ){
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("RECEIVED LAST MESSAGE! "+totalRead.get()+"  --- "+timesReceived);
        timesReceived = 0;
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        EOSFunction.execute(ctx.name());
        System.out.println("TOOK READING TIME: "+timeElapsed);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
