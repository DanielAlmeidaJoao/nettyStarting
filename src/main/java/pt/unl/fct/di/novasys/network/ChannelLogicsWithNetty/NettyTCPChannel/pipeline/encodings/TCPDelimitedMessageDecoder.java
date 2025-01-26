package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.pipeline.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.TCPNettyImplementation.StreamingNettyConsumer;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.pipeline.AbstractMessageDecoderHandler;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.NewChannelsFactoryUtils;

public class TCPDelimitedMessageDecoder extends AbstractMessageDecoderHandler {
    public final TransmissionType type;
    public static final String NAME = "TCPDelimitedMessageDecoder";
    private final BabelMessageSerializer serializer;

    public TCPDelimitedMessageDecoder(StreamingNettyConsumer consumer) {
        super(consumer);
        type = TransmissionType.STRUCTURED_MESSAGE;
        serializer = consumer.getSerializer();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        //System.out.println(getClass().getName()+": "+cause.getMessage());
        consumer.channelError(null,cause,ctx.channel().id().asShortText());
        NewChannelsFactoryUtils.closeOnError(ctx.channel());
    }
    @Override
    public boolean handleReceivedMessage(ChannelHandlerContext ctx, ByteBuf in, int len) {
        try{
            BabelMessage babelMessage = serializer.deserialize(in.readSlice(len));
            consumer.onChannelMessageRead(ctx.channel().id().asShortText(),babelMessage,len+1);
        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
        return true;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        consumer.onChannelInactive(ctx.channel().id().asShortText());
    }
}
