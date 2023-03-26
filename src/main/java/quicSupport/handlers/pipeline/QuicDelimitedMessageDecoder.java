package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.CustomQuicChannel;
import quicSupport.handlers.nettyFuncHandlers.QuicListenerExecutor;
import quicSupport.utils.Logics;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.util.List;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);

    private final boolean incoming;
    private QuicListenerExecutor streamListenerExecutor;
    private final QuicChannelMetrics metrics;

    public QuicDelimitedMessageDecoder(QuicListenerExecutor streamListenerExecutor, QuicChannelMetrics metrics, boolean incoming){
        this.incoming=incoming;
        this.streamListenerExecutor=streamListenerExecutor;
        this.metrics=metrics;
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

        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        if(Logics.APP_DATA==msgType){
            streamListenerExecutor.onChannelRead(ch.id().asShortText(),data);
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedAppMessages(q.getReceivedAppMessages()+1);
                q.setReceivedAppBytes(q.getReceivedAppBytes()+length+Logics.WRT_OFFSET);
            }
        }else if(Logics.KEEP_ALIVE==msgType){
            logger.info("HEART BEAT RECEIVED INCOMING ? {}",incoming);
            streamListenerExecutor.onKeepAliveMessage(ch.parent().id().asShortText());
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedKeepAliveMessages(1+q.getReceivedKeepAliveMessages());
            }
        }else{
            streamListenerExecutor.onChannelActive(ch,data,null);
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedControlMessages(q.getReceivedControlMessages()+1);
                q.setReceivedControlBytes(q.getReceivedControlBytes()+length+Logics.WRT_OFFSET);
            }
        }
    }
}
