package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamChannelConfig;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logics;
import quicSupport.utils.entities.MessageDecoderOutput;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefautQuicStreamReadHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicServerExample.class);
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';
    private final QuicListenerExecutor streamListenerExecutor;
    private final QuicChannelMetrics metrics;
    private final boolean incoming;
    private ScheduledFuture scheduledFuture;
    private boolean canSendHeartBeat;

    public DefautQuicStreamReadHandler(QuicListenerExecutor streamListenerExecutor, QuicChannelMetrics metrics, boolean incoming) {
        this.streamListenerExecutor = streamListenerExecutor;
        this.metrics = metrics;
        this.incoming = incoming;
        scheduledFuture = null;
        canSendHeartBeat=false;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        QuicStreamChannelConfig config = ch.config();
        config.setAllowHalfClosure(false);
        if(metrics!=null){
            QuicConnectionMetrics m = metrics.getConnectionMetrics(ch.parent().remoteAddress());
            m.setCreatedStreamCount(m.getCreatedStreamCount()+1);
        }
        streamListenerExecutor.onStreamCreated(ch);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        streamListenerExecutor.onStreamClosed((QuicStreamChannel) ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MessageDecoderOutput message = (MessageDecoderOutput) msg;
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        if(Logics.APP_DATA == message.getMsgCode()) {
            readAppData(ch,message);
        } else if (Logics.KEEP_ALIVE==message.getMsgCode()) {
            readKeepAliveMessage(ch);
        } else if (Logics.HANDSHAKE_MESSAGE==message.getMsgCode()) {
            readHandShakeData(ch,message);
        }else{
            throw new AssertionError("Unknown msg code in encoder: " + ((MessageDecoderOutput) msg).getMsgCode());
        }
        if(incoming&&canSendHeartBeat){
            scheduleSendHeartBeat_KeepAlive(ch);
            canSendHeartBeat=false;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        cause.printStackTrace();
        streamListenerExecutor.onStreamError(ch,cause);
    }
    public void readAppData(QuicStreamChannel ch, MessageDecoderOutput message){
        streamListenerExecutor.onChannelRead(ch.id().asShortText(),message.getData());
        if(metrics!=null){
            QuicConnectionMetrics q = metrics.getConnectionMetrics(ch.parent().remoteAddress());
            q.setReceivedAppMessages(q.getReceivedAppMessages()+1);
            q.setReceivedAppBytes(q.getReceivedAppBytes()+message.getData().length+Logics.WRT_OFFSET);
        }
    }
    private void readKeepAliveMessage(QuicStreamChannel ch){
        logger.info("HEART BEAT RECEIVED INCOMING ? {}",incoming);
        if(metrics!=null){
            QuicConnectionMetrics q = metrics.getConnectionMetrics(ch.parent().remoteAddress());
            q.setReceivedKeepAliveMessages(1+q.getReceivedKeepAliveMessages());
        }
        if(incoming==Logics.OUTGOING_CONNECTION){
            scheduleSendHeartBeat_KeepAlive(ch);
            return;
        }else{//heartbeat reply
            canSendHeartBeat=true;
        }
    }
    private void readHandShakeData(QuicStreamChannel ch, MessageDecoderOutput message){
        streamListenerExecutor.onChannelActive(ch,message.getData(),null);
        if(metrics!=null){
            QuicConnectionMetrics q = metrics.getConnectionMetrics(ch.parent().remoteAddress());
            q.setReceivedControlMessages(q.getReceivedControlMessages()+1);
            q.setReceivedControlBytes(q.getReceivedControlBytes()+message.getData().length+Logics.WRT_OFFSET);
        }
    }
    private void scheduleSendHeartBeat_KeepAlive(QuicStreamChannel streamChannel){
        if(scheduledFuture!=null){
            scheduledFuture.cancel(true);
        }
        scheduledFuture = streamChannel.eventLoop().schedule(() -> {
            logger.info("HEART BEAT SENT. INCOMING ? {}",incoming);
            streamChannel.writeAndFlush(Logics.writeBytes(1,"a".getBytes(),Logics.KEEP_ALIVE));
        }, (long) (Logics.maxIdleTimeoutInSeconds*0.75), TimeUnit.SECONDS);
    }
}
