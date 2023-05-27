package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.ConnectionOrStreamType;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.net.InetSocketAddress;
import java.util.Map;

public class QuicClientChannelConHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(QuicClientChannelConHandler.class);
    private final InetSocketAddress self;
    private final InetSocketAddress remote;
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;
    private final ConnectionOrStreamType connectionOrStreamType;

    public QuicClientChannelConHandler(InetSocketAddress self, InetSocketAddress remote, CustomQuicChannelConsumer consumer, QuicChannelMetrics  metrics, ConnectionOrStreamType connectionOrStreamType) {
        this.self = self;
        this.remote = remote;
        this.consumer = consumer;
        this.metrics = metrics;
        this.connectionOrStreamType = connectionOrStreamType;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("{} ESTABLISHED CONNECTION WITH {}",self,remote);
        QuicChannel out = (QuicChannel) ctx.channel();
        if(metrics!=null){
            metrics.initConnectionMetrics(out.remoteAddress());
        }
        QuicStreamChannel streamChannel = QUICLogics.createStream(out,consumer,metrics,false);
        QuicHandShakeMessage handShakeMessage = new QuicHandShakeMessage(self.getHostName(),self.getPort(),streamChannel.id().asShortText(),connectionOrStreamType);
        byte [] hs = QUICLogics.gson.toJson(handShakeMessage).getBytes();
        streamChannel.writeAndFlush(QUICLogics.writeBytes(hs.length,hs, QUICLogics.HANDSHAKE_MESSAGE))
                .addListener(future -> {
                    if(future.isSuccess()){
                        consumer.channelActive(streamChannel,null,remote);
                        System.out.println("BEFORE");
                        for (Map.Entry<String, ChannelHandler> stringChannelHandlerEntry : streamChannel.pipeline()) {
                            System.out.println(stringChannelHandlerEntry.getKey()+" "+stringChannelHandlerEntry.getValue());
                        }
                        streamChannel.pipeline().replace(QuicMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                        streamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(consumer,metrics,false));
                        System.out.println("AFTER");
                        for (Map.Entry<String, ChannelHandler> stringChannelHandlerEntry : streamChannel.pipeline()) {
                            System.out.println(stringChannelHandlerEntry.getKey()+" "+stringChannelHandlerEntry.getValue());
                        }
                    }else{
                        logger.info("{} CONNECTION TO {} COULD NOT BE ACTIVATED.",self,remote);
                        consumer.streamErrorHandler(streamChannel,future.cause());
                        future.cause().printStackTrace();
                        out.close();
                    }
                });
        logger.debug("{} SENT CUSTOM HANDSHAKE DATA TO {}",self,remote);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.onConnectionClosed(ctx.channel().remoteAddress());
        }
        consumer.channelInactive(ctx.channel().id().asShortText());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        consumer.handleOpenConnectionFailed((InetSocketAddress) ctx.channel().remoteAddress(),cause);
        cause.printStackTrace();
    }
}
