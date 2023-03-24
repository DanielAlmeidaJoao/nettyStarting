package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logics;
import quicSupport.utils.entities.MessageDecoderOutput;
import quicSupport.utils.entities.QuicChannelMetrics;

public class NonDefaultQuicStreamReadHandler extends DefautQuicStreamReadHandler {


    public NonDefaultQuicStreamReadHandler(QuicListenerExecutor streamListenerExecutor, QuicChannelMetrics metrics, boolean incoming) {
        super(streamListenerExecutor,metrics,incoming);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MessageDecoderOutput message = (MessageDecoderOutput) msg;
        if(Logics.APP_DATA==message.getMsgCode()){
            readAppData((QuicStreamChannel) ctx.channel(),message);
        }else {
            throw new AssertionError("Unknown msg code in encoder: " + ((MessageDecoderOutput) msg).getMsgCode());
        }
    }

}
