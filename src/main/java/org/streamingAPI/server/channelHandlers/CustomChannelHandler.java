package org.streamingAPI.server.channelHandlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.streamingAPI.handlerFunctions.receiver.HandlerFunctions;

import java.util.concurrent.atomic.AtomicLong;

//@ChannelHandler.Sharable
public abstract class CustomChannelHandler extends ChannelHandlerAdapter {

    private long timeElapsed;
    private int totalRead;
    private HandlerFunctions handlerFunctions;
    private int timesReceived = 0;
    private int timesCompleted = 0;


    private ByteBuf tmp;

    public CustomChannelHandler(HandlerFunctions handlerFunctions){
        this.handlerFunctions  = handlerFunctions;
        totalRead = 0;
        timeElapsed = 0;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        handlerFunctions.getActiveFunction().execute(ctx.channel().id().asShortText());
    }
    private void deliverData(ByteBuf in, String streamId){
        //System.out.println("ALSO RECEIVED!!!");
        try {
            //while (in.isReadable()) {
            long start = System.currentTimeMillis();
            byte[] bytes = new byte[in.readableBytes()];
            long end = System.currentTimeMillis();
            timeElapsed += (end-start);
            in.readBytes(bytes);
            handlerFunctions.getFunctionToExecute().execute(streamId,bytes);
            totalRead += bytes.length;
            timesReceived++;
            //}
        }catch (Exception e ){
            e.printStackTrace();
        }finally {
            ReferenceCountUtil.release(tmp);
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        tmp = in;
        deliverData(in,ctx.channel().id().asShortText());
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        timesCompleted++;
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        handlerFunctions.getEOSFunction().execute(ctx.channel().id().asShortText());
        System.out.printf("CHANNEL %S CLOSED. TOOK READING TIME: %S. TOTAL READ %S \n",ctx.channel().id().asShortText(),timeElapsed+"",totalRead+"");
        System.out.println("TIMES READ: "+timesReceived+" TIMES COMPLETED: "+timesCompleted);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
