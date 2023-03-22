package quicSupport.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logics;
import quicSupport.utils.entities.ControlDataEntity;
import quicSupport.utils.entities.QuicChannelMetrics;

import java.util.List;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {

    private QuicListenerExecutor streamListenerExecutor;
    public QuicDelimitedMessageDecoder(QuicListenerExecutor streamListenerExecutor){
        this.streamListenerExecutor=streamListenerExecutor;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if(msg.readableBytes()<4){
            return;
        }
        msg.markReaderIndex();
        int length = msg.readInt();

        if(msg.readableBytes()<length+1){
            msg.resetReaderIndex();
            return;
        }
        byte msgType = msg.readByte();
        byte [] data = new byte[length];
        msg.readBytes(data);
        if(Logics.APP_DATA==msgType){
            QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
            streamListenerExecutor.onChannelRead(ch.id().asShortText(),data);
        }else{
            ControlDataEntity controlData = new ControlDataEntity(null,data);
            streamListenerExecutor.onChannelActive((QuicStreamChannel) ctx.channel(),controlData,length+5,true);
        }
    }
}
