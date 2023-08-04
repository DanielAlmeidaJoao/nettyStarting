package tcpSupport.tcpChannelAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;

import java.util.List;

//@ChannelHandler.Sharable
public abstract class AbstractMessageDecoderHandler extends ByteToMessageDecoder {

    public static final String NAME ="CHSHAKE_HANDLER";
    protected final StreamingNettyConsumer consumer;
    private int len;
    public AbstractMessageDecoderHandler(StreamingNettyConsumer consumer){
        this.consumer = consumer;
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        boolean iterate = true;
        while (in.readableBytes()>=Integer.BYTES && iterate) {
            in.markReaderIndex();
            len = in.readInt();
            if(in.readableBytes()<len){
                in.resetReaderIndex();
                return;
            }
            iterate = handleReceivedMessage(ctx,in,len);
        }
    }
    public abstract boolean handleReceivedMessage(ChannelHandlerContext ctx, ByteBuf in, int len);
}
