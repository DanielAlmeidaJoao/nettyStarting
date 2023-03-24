package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import pt.unl.fct.di.novasys.channel.tcp.events.ChannelMetrics;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logics;
import quicSupport.utils.entities.ControlDataEntity;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.util.List;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {

    private QuicListenerExecutor streamListenerExecutor;
    private final QuicChannelMetrics metrics;

    public QuicDelimitedMessageDecoder(QuicListenerExecutor streamListenerExecutor, QuicChannelMetrics metrics){
        System.out.println("DELIMITER DECODER INITIED");
        this.streamListenerExecutor=streamListenerExecutor;
        this.metrics = metrics;
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
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedAppMessages(q.getReceivedAppMessages()+1);
                q.setReceivedAppBytes(q.getReceivedAppBytes()+length+Logics.WRT_OFFSET);
            }
        }else{
            streamListenerExecutor.onChannelActive((QuicStreamChannel) ctx.channel(),data,null);
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedControlMessages(q.getReceivedControlMessages()+1);
                q.setReceivedControlBytes(q.getReceivedControlBytes()+length+Logics.WRT_OFFSET);
            }
        }
    }
}
