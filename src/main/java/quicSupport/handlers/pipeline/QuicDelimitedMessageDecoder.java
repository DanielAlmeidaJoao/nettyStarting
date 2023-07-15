package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.channels.NettyQUICChannel;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.util.List;

import static quicSupport.utils.QUICLogics.gson;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(NettyQUICChannel.class);
    public static final String HANDLER_NAME="QuicDelimitedMessageDecoder";
    private final boolean incoming;
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;

    public QuicDelimitedMessageDecoder(CustomQuicChannelConsumer streamListenerExecutor, QuicChannelMetrics metrics, boolean incoming){
        this.incoming=incoming;
        this.consumer=streamListenerExecutor;
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
        msg.discardReadBytes();
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        if(QUICLogics.APP_DATA==msgType){
            consumer.onReceivedDelimitedMessage(ch.id().asShortText(),data);
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedAppMessages(q.getReceivedAppMessages()+1);
                q.setReceivedAppBytes(q.getReceivedAppBytes()+length+ QUICLogics.WRT_OFFSET);
            }
        }else if(QUICLogics.KEEP_ALIVE==msgType){
            consumer.onKeepAliveMessage(ch.parent().id().asShortText());
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedKeepAliveMessages(1+q.getReceivedKeepAliveMessages());
            }
        }else if(QUICLogics.STREAM_CREATED==msgType){
            msg = Unpooled.copiedBuffer(data);
            int ordinal = msg.readInt();
            msg.release();
            TransmissionType type;
            if(TransmissionType.UNSTRUCTURED_STREAM.ordinal() == ordinal){
                type = TransmissionType.UNSTRUCTURED_STREAM;
                ch.pipeline().remove(QuicStructuredMessageEncoder.HANDLER_NAME);
                ch.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(consumer,metrics,false));
            }else{
                type = TransmissionType.STRUCTURED_MESSAGE;
            }
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedControlMessages(q.getReceivedControlMessages()+1);
                q.setReceivedControlBytes(q.getReceivedControlBytes()+length+ QUICLogics.WRT_OFFSET);
            }
            ((QuicStreamReadHandler) ch.pipeline().get(QuicStreamReadHandler.HANDLER_NAME)).notifyAppDelimitedStreamCreated(ch,type,consumer.nextId(),true);
        }else if(QUICLogics.HANDSHAKE_MESSAGE==msgType){
            QuicHandShakeMessage handShakeMessage = gson.fromJson(new String(data), QuicHandShakeMessage.class);

            if(TransmissionType.UNSTRUCTURED_STREAM==handShakeMessage.transmissionType){
                ch.pipeline().remove(QuicStructuredMessageEncoder.HANDLER_NAME);
                ch.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(consumer,metrics,true));
            }
            consumer.channelActive(ch,handShakeMessage,null, TransmissionType.STRUCTURED_MESSAGE);
            if(metrics!=null){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(ctx.channel().parent().remoteAddress());
                q.setReceivedControlMessages(q.getReceivedControlMessages()+1);
                q.setReceivedControlBytes(q.getReceivedControlBytes()+length+ QUICLogics.WRT_OFFSET);
            }
        }else{
            throw new AssertionError("RECEIVED UNKNOW MESSAGE TYPE: "+msgType);
        }
        //ctx.fireChannelRead(msg);
    }    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

}
