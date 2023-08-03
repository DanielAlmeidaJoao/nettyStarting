package tcpSupport.tcpChannelAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

import java.util.List;

public class TCPStreamMessageDecoder extends SimpleChannelInboundHandler {

    public static final String NAME="TCPStreamMessageDecoder";
    public final StreamingNettyConsumer consumer;
    public final TransmissionType type;

    public TCPStreamMessageDecoder(StreamingNettyConsumer consumer) {
        this.consumer = consumer;
        type = TransmissionType.UNSTRUCTURED_STREAM;
    }

    //@Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out){
        int available = in.readableBytes();
        BabelOutputStream babelOutputStream = new BabelOutputStream(in.retainedDuplicate(),available);
        in.readerIndex(available);
        consumer.onChannelStreamRead(ctx.channel().id().asShortText(),babelOutputStream);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        //cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        int available = in.readableBytes();
        BabelOutputStream babelOutputStream = new BabelOutputStream(in.retainedDuplicate(),available);
        in.readerIndex(available);
        consumer.onChannelStreamRead(ctx.channel().id().asShortText(),babelOutputStream);
    }
}
