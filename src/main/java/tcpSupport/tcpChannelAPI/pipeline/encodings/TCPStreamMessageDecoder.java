package tcpSupport.tcpChannelAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;
import tcpSupport.tcpChannelAPI.utils.NewChannelsFactoryUtils;

import java.util.List;

public class TCPStreamMessageDecoder extends ByteToMessageDecoder {

    public static final String NAME="TCPStreamMessageDecoder";
    public final StreamingNettyConsumer consumer;
    public final TransmissionType type;

    public TCPStreamMessageDecoder(StreamingNettyConsumer consumer) {
        this.consumer = consumer;
        type = TransmissionType.UNSTRUCTURED_STREAM;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out){
        int available = in.readableBytes();
        BabelOutputStream babelOutputStream = new BabelOutputStream(in.retainedDuplicate(),available);
        in.readerIndex(available);
        consumer.onChannelStreamRead(ctx.channel().id().asShortText(),babelOutputStream);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        System.out.println(getClass().getName()+": "+cause.getMessage());
        consumer.channelError(null,cause,ctx.channel().id().asShortText());
        NewChannelsFactoryUtils.closeOnError(ctx.channel());
    }

    /**
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        int available = in.readableBytes();
        BabelOutputStream babelOutputStream = new BabelOutputStream(in.retainedDuplicate(),available);
        in.readerIndex(available);
        consumer.onChannelStreamRead(ctx.channel().id().asShortText(),babelOutputStream);
    }
    **/

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        consumer.onChannelInactive(ctx.channel().id().asShortText());
    }
}
