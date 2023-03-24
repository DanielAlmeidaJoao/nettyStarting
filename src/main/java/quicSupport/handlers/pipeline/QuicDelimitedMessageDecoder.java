package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.CustomQuicChannel;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logics;
import quicSupport.utils.entities.MessageDecoderOutput;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);
    /**
    private final boolean incoming;
    private QuicListenerExecutor streamListenerExecutor;
    private final QuicChannelMetrics metrics;
    private ScheduledFuture scheduledFuture;
    private boolean canSendHeartBeat; **/

    public QuicDelimitedMessageDecoder(){}

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
        MessageDecoderOutput messageDecoderOutput = new MessageDecoderOutput(msgType,data);
        out.add(messageDecoderOutput);
        /**
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
        if(Logics.APP_DATA==msgType){
            streamListenerExecutor.onChannelRead(ch.id().asShortText(),data);
            if(metrics!=null){
                q.setReceivedAppMessages(q.getReceivedAppMessages()+1);
                q.setReceivedAppBytes(q.getReceivedAppBytes()+length+Logics.WRT_OFFSET);
            }
        }else if(Logics.KEEP_ALIVE==msgType){
            logger.info("HEART BEAT RECEIVED INCOMING ? {}",incoming);
            if(metrics!=null){
                q.setReceivedKeepAliveMessages(1+q.getReceivedKeepAliveMessages());
            }
            if(incoming==Logics.OUTGOING_CONNECTION){
                scheduleSendHeartBeat_KeepAlive(ch);
                return;
            }else{//heartbeat reply
                canSendHeartBeat=true;
            }
        }else{
            streamListenerExecutor.onChannelActive(ch,data,null);
            if(metrics!=null){
                q.setReceivedControlMessages(q.getReceivedControlMessages()+1);
                q.setReceivedControlBytes(q.getReceivedControlBytes()+length+Logics.WRT_OFFSET);
            }
        }
        if(incoming&&canSendHeartBeat){
            scheduleSendHeartBeat_KeepAlive(ch);
            canSendHeartBeat=false;
        } **/
    }
}
