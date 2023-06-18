package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannel;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.ConnectionId;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.util.List;

import static quicSupport.utils.QUICLogics.gson;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);
    public static final String HANDLER_NAME="QuicDelimitedMessageDecoder";
    private final boolean incoming;
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;
    private ConnectionId identification;

    public QuicDelimitedMessageDecoder(CustomQuicChannelConsumer streamListenerExecutor, QuicChannelMetrics metrics, boolean incoming, ConnectionId identification){
        this.incoming=incoming;
        this.consumer=streamListenerExecutor;
        this.metrics=metrics;
        this.identification = identification;
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
        msg.discardSomeReadBytes();
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        if(QUICLogics.APP_DATA==msgType){
            consumer.onReceivedDelimitedMessage(identification,data);
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedAppMessages(q.getReceivedAppMessages()+1);
                q.setReceivedAppBytes(q.getReceivedAppBytes()+length+ QUICLogics.WRT_OFFSET);
            }
        }else if(QUICLogics.KEEP_ALIVE==msgType){
            consumer.onKeepAliveMessage(identification);
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedKeepAliveMessages(1+q.getReceivedKeepAliveMessages());
            }
        }else if(QUICLogics.STREAM_CREATED==msgType){
            msg = Unpooled.copiedBuffer(data);
            int ordinal = msg.readInt();
            TransmissionType type;
            Triple triple = null;
            if(TransmissionType.UNSTRUCTURED_STREAM.ordinal() == ordinal){
                type = TransmissionType.UNSTRUCTURED_STREAM;
                ch.pipeline().replace(QuicMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                ch.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(consumer,metrics,identification));
                short sourceProto = msg.readShort();
                short destProto = msg.readShort();
                short handlerId = msg.readShort();
                msg.release();
                triple = Triple.of(sourceProto,destProto,handlerId);
            }else{
                type = TransmissionType.STRUCTURED_MESSAGE;
            }
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedControlMessages(q.getReceivedControlMessages()+1);
                q.setReceivedControlBytes(q.getReceivedControlBytes()+length+ QUICLogics.WRT_OFFSET);
                q.setStreamCount(q.getStreamCount()+1);
            }
            ((QuicStreamHandler) ch.pipeline().get(QuicStreamHandler.HANDLER_NAME)).notifyAppDelimitedStreamCreated(ch,type,triple,false);
        }else if(QUICLogics.HANDSHAKE_MESSAGE==msgType){
            assert identification == null;
            QuicHandShakeMessage handShakeMessage = gson.fromJson(new String(data),QuicHandShakeMessage.class);
            identification = ConnectionId.of(handShakeMessage.getAddress(),consumer.nextId());
            consumer.channelActive(ch,handShakeMessage,identification, TransmissionType.STRUCTURED_MESSAGE);
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedControlMessages(q.getReceivedControlMessages()+1);
                q.setReceivedControlBytes(q.getReceivedControlBytes()+length+ QUICLogics.WRT_OFFSET);
            }
        }else{
            throw new AssertionError("RECEIVED UNKNOW MESSAGE TYPE: "+msgType);
        }
    }    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

}
