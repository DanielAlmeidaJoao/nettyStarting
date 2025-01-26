package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.TCPNettyImplementation.StreamingNettyConsumer;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.BabelOutputStream;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.NewChannelsFactoryUtils;

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
        //System.out.println(getClass().getName()+": "+cause.getMessage());
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
