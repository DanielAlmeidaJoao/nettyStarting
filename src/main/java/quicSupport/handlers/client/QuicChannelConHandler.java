package quicSupport.handlers.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.handlers.server.QuicStreamReadHandler;
import quicSupport.utils.Logics;

import java.net.InetSocketAddress;

public class QuicChannelConHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(QuicChannelConHandler.class);
    private final InetSocketAddress self;
    private final InetSocketAddress remote;
    private final QuicListenerExecutor quicListenerExecutor;
    private final boolean metricsOn;

    public QuicChannelConHandler(InetSocketAddress self, InetSocketAddress remote, QuicListenerExecutor streamListenerExecutor, boolean metricsEnabled) {
        this.self = self;
        this.remote = remote;
        this.quicListenerExecutor = streamListenerExecutor;
        metricsOn=metricsEnabled;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        QuicChannel out = (QuicChannel) ctx.channel();
        logger.info("{} ESTABLISHED CONNECTION WITH {}",self,remote);
        HandShakeMessage handShakeMessage = new HandShakeMessage(self.getHostName(),self.getPort());
        byte [] hs = Logics.gson.toJson(handShakeMessage).getBytes();
        QuicStreamChannel streamChannel = Logics.createStream((QuicChannel) ctx.channel(),quicListenerExecutor);
        streamChannel.writeAndFlush(Logics.writeBytes(hs.length,hs, QuicStreamReadHandler.HANDSHAKE_MESSAGE))
                .addListener(future -> {
                    if(future.isSuccess()){
                        HandShakeMessage hsm = new HandShakeMessage(remote.getHostName(),remote.getPort());
                        quicListenerExecutor.onChannelActive(streamChannel,hsm,false);
                    }else{
                        logger.info("{} CONNECTION TO {} COULD NOT BE ACTIVATED.",self,remote);
                        quicListenerExecutor.onConnectionError(remote,future.cause());
                        out.close();
                    }
                });
        logger.info("{} SENT CUSTOM HANDSHAKE DATA TO {}",self,remote);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("CLIENT CHANNEL INACTIVE");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        quicListenerExecutor.onConnectionError((InetSocketAddress) ctx.channel().remoteAddress(),cause);
        cause.printStackTrace();
    }
}
