package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
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

import java.util.List;

import static quicSupport.utils.QUICLogics.gson;

public class QuicDelimitedMessageDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(NettyQUICChannel.class);
    public static final String HANDLER_NAME="QuicDelimitedMessageDecoder";
    private final boolean incoming;
    private final CustomQuicChannelConsumer consumer;
    private final String customId;


    public QuicDelimitedMessageDecoder(CustomQuicChannelConsumer streamListenerExecutor, boolean incoming, String customId){
        this.incoming=incoming;
        this.consumer=streamListenerExecutor;
        this.customId = customId;
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
        msg = msg.readBytes(length);
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        if(QUICLogics.APP_DATA==msgType){
            consumer.onReceivedDelimitedMessage(customId,msg);
        }else if(QUICLogics.KEEP_ALIVE==msgType){
            msg.readByte();
            consumer.onKeepAliveMessage(customId,length+1);
        }else if(QUICLogics.STREAM_CREATED==msgType){
            //msg = Unpooled.wrappedBuffer(data);
            int ordinal = msg.readInt();
            //msg.discardReadBytes();
            TransmissionType type;
            if(TransmissionType.UNSTRUCTURED_STREAM.ordinal() == ordinal){
                type = TransmissionType.UNSTRUCTURED_STREAM;
                ch.pipeline().remove(QuicStructuredMessageEncoder.HANDLER_NAME);
                ch.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(consumer, false,customId));
            }else{
                type = TransmissionType.STRUCTURED_MESSAGE;
            }
            consumer.streamCreatedHandler(ch,type,customId,true);
        }else if(QUICLogics.HANDSHAKE_MESSAGE==msgType){
            byte [] data = new byte[length];
            msg.readBytes(data);
            QuicHandShakeMessage handShakeMessage = gson.fromJson(new String(data), QuicHandShakeMessage.class);
            if(TransmissionType.UNSTRUCTURED_STREAM==handShakeMessage.transmissionType){
                ch.pipeline().remove(QuicStructuredMessageEncoder.HANDLER_NAME);
                ch.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(consumer, true, customId));
            }
            consumer.channelActive(ch,handShakeMessage,null, TransmissionType.STRUCTURED_MESSAGE,length, customId);
        }else{
            throw new AssertionError("RECEIVED UNKNOW MESSAGE TYPE: "+msgType);
        }
        msg.discardReadBytes();
        msg.release();
        //ctx.fireChannelRead(msg);
    }    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

}
