package org.example.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@ChannelHandler.Sharable
public class StreamReceiverHandler extends ChannelHandlerAdapter {
    private AtomicLong totalRead;
    int timesReceived = 0;
    FileOutputStream fos;
    public StreamReceiverHandler(){
        totalRead = new AtomicLong(0);
        try {
            //String inputFileName = "/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4";
            fos = new FileOutputStream("ola2_movie.mp4");
        }catch (Exception e){
            fos = null;
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;

        try {
            while (in.isReadable()) {
                byte[] bytes = new byte[in.readableBytes()];
                in.readBytes(bytes);
                totalRead.set(totalRead.get() + bytes.length);
                timesReceived++;
                fos.write(bytes, 0, bytes.length);
                fos.flush();
            }
        }catch (Exception e ){
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }
        //System.out.println("Server received: " + in.toString(CharsetUtil.UTF_8));

        //ctx.writeAndFlush(in);
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        //ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        System.out.println("RECEIVED LAST MESSAGE! "+totalRead.get()+"  --- "+timesReceived);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
