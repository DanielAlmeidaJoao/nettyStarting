package quicSupport.handlers.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.client_server.QuicClientExample;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.utils.Logic;

import java.net.InetSocketAddress;

public class QuicChannelConHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(QuicChannelConHandler.class);
    private final InetSocketAddress self;
    private final InetSocketAddress remote;
    private final QuicListenerExecutor quicListenerExecutor;

    public QuicChannelConHandler(InNettyChannelListener listener, InetSocketAddress self, InetSocketAddress remote, QuicListenerExecutor streamListenerExecutor) {
        this.self = self;
        this.remote = remote;
        this.quicListenerExecutor = streamListenerExecutor;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        QuicChannel out = (QuicChannel) ctx.channel();
        logger.info("{} ESTABLISHED CONNECTION WITH {}",self,remote);
        // As we did not allow any remote initiated streams we will never see this method called.
        // That said just let us keep it here to demonstrate that this handle would be called
        // for each remote initiated stream.
        HandShakeMessage handShakeMessage = new HandShakeMessage(self.getHostName(),self.getPort());
        byte [] hs = Logic.gson.toJson(handShakeMessage).getBytes();
        QuicStreamChannel streamChannel = QuicClientExample.createStream((QuicChannel) ctx.channel(),new QuicStreamReadHandler( quicListenerExecutor));
        streamChannel.writeAndFlush(Unpooled.copiedBuffer(hs))
                .addListener(future -> {
                    if(future.isSuccess()){
                        HandShakeMessage hsm = new HandShakeMessage(remote.getHostName(),remote.getPort());
                        quicListenerExecutor.onChannelActive(streamChannel,hsm);
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
        System.out.println("CHANNEL INACTIVE CLIENT");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        quicListenerExecutor.onConnectionError((InetSocketAddress) ctx.channel().remoteAddress(),cause);
        cause.printStackTrace();
    }
}
