package tcpSupport.tcpChannelAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;

import java.net.UnknownHostException;

//@ChannelHandler.Sharable
public class TCPClientNettyHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(TCPClientNettyHandler.class);

    private HandShakeMessage handshakeData;
    private final TransmissionType type;
    private final StreamingNettyConsumer consumer;


    public TCPClientNettyHandler(HandShakeMessage handshakeData, StreamingNettyConsumer consumer, TransmissionType type){
        this.consumer = consumer;
        this.handshakeData = handshakeData;
        this.type = type;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws UnknownHostException {
        byte [] data = TCPChannelUtils.g.toJson(handshakeData).getBytes();
        ByteBuf tmp = ctx.alloc().buffer(data.length+4);
        tmp.writeInt(data.length);
        tmp.writeBytes(data);

        ctx.writeAndFlush(tmp).addListener(future -> {
            if(!future.isSuccess()){
                //future.cause().printStackTrace();
                ctx.channel().close();
            }
        });
        /**
        if(TransmissionType.UNSTRUCTURED_STREAM == type){
            ctx.channel().pipeline().replace(TCPDelimitedMessageDecoder.NAME, TCPStreamMessageDecoder.NAME,new TCPStreamMessageDecoder(consumer));
        }**/
        consumer.onChannelActive(ctx.channel(),null,type,data.length);
        handshakeData=null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        consumer.onConnectionFailed(ctx.channel().id().asShortText(),cause,type);
        logger.error(cause.getMessage());
        TCPChannelUtils.closeOnError(ctx.channel());
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        consumer.onChannelInactive(ctx.channel().id().asShortText());
    }
}