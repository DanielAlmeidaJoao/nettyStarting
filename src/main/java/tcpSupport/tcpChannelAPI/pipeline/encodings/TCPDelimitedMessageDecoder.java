package tcpSupport.tcpChannelAPI.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.pipeline.AbstractMessageDecoderHandler;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;

public class TCPDelimitedMessageDecoder extends AbstractMessageDecoderHandler {
    public final TransmissionType type;
    public static final String NAME = "TCPDelimitedMessageDecoder";

    public TCPDelimitedMessageDecoder(StreamingNettyConsumer consumer) {
        super(consumer);
        type = TransmissionType.STRUCTURED_MESSAGE;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        System.out.println(getClass().getName()+": "+cause.getMessage());
        consumer.channelError(null,cause,ctx.channel().id().asShortText());
        TCPChannelUtils.closeOnError(ctx.channel());
    }

    @Override
    public boolean handleReceivedMessage(ChannelHandlerContext ctx, ByteBuf in, int len) {
        ByteBuf buf = in.readBytes(len);
        in.discardReadBytes();
        consumer.onChannelMessageRead(ctx.channel().id().asShortText(),buf);
        buf.release();
        return true;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        consumer.onChannelInactive(ctx.channel().id().asShortText());
    }
}
