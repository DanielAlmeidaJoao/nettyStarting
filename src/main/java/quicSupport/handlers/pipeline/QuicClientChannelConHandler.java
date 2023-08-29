package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;

import java.net.InetSocketAddress;

public class QuicClientChannelConHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(QuicClientChannelConHandler.class);
    private final InetSocketAddress self;
    private final InetSocketAddress remote;
    private final CustomQuicChannelConsumer consumer;
    private final TransmissionType transmissionType;
    private final short destProto;

    public QuicClientChannelConHandler(InetSocketAddress self, InetSocketAddress remote, CustomQuicChannelConsumer consumer, TransmissionType transmissionType, short destProto) {
        this.self = self;
        this.remote = remote;
        this.consumer = consumer;
        this.transmissionType = transmissionType;
        this.destProto = destProto;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("{} ESTABLISHED CONNECTION WITH {}",self,remote);
        QuicChannel out = (QuicChannel) ctx.channel();
        final String customConId = out.attr(AttributeKey.valueOf(TCPChannelUtils.CUSTOM_ID_KEY)).getAndSet(null).toString();

        QuicStreamChannel streamChannel = out
                .createStream(QuicStreamType.BIDIRECTIONAL, new QuicStreamInboundHandler(consumer, customConId, QUICLogics.OUTGOING_CONNECTION))
                .sync()
                .getNow();
        final QuicHandShakeMessage handShakeMessage = new QuicHandShakeMessage(self.getHostName(),self.getPort(),streamChannel.id().asShortText(),transmissionType,destProto);
        byte [] hs = TCPChannelUtils.g.toJson(handShakeMessage).getBytes();
        ByteBuf byteBuf = ctx.alloc().directBuffer(hs.length+1);
        byteBuf.writeInt(hs.length);
        byteBuf.writeByte(QUICLogics.HANDSHAKE_MESSAGE);
        byteBuf.writeBytes(hs);
        streamChannel.writeAndFlush(byteBuf)
                .addListener(future -> {
                    if(future.isSuccess()){
                        if(TransmissionType.UNSTRUCTURED_STREAM==handShakeMessage.transmissionType){
                            streamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(consumer,false,customConId));
                        }
                        consumer.channelActive(streamChannel,null,remote, transmissionType,hs.length,customConId);
                    }else{
                        logger.info("{} CONNECTION TO {} COULD NOT BE ACTIVATED.",self,remote);
                        consumer.streamErrorHandler(streamChannel,future.cause(), customConId);
                        future.cause().printStackTrace();
                        out.close();
                    }
                });
        //logger.debug("{} SENT CUSTOM HANDSHAKE DATA TO {}",self,remote);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        consumer.channelInactive(ctx.channel().id().asShortText());
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //consumer.handleOpenConnectionFailed((InetSocketAddress) ctx.channel().remoteAddress(),cause, transmissionType, id);
        cause.printStackTrace();
    }
}
