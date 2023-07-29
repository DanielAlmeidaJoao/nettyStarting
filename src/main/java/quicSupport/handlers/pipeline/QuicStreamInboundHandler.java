package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.client_server.QuicServerExample;
import quicSupport.utils.customConnections.CustomQUICStreamCon;

public class QuicStreamInboundHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicServerExample.class);
    public static final String HANDLER_NAME="QuicStreamInboundHandler";
    private final String customId;
    private final boolean inConnection;
    private CustomQUICStreamCon streamCon;

    private final CustomQuicChannelConsumer consumer;

    public QuicStreamInboundHandler(CustomQuicChannelConsumer streamListenerExecutor, String id, boolean incommingCon) {
        this.consumer = streamListenerExecutor;
        this.customId = id;
        this.inConnection = incommingCon;
    }
    public void setStreamCon(CustomQUICStreamCon con ){
        if(streamCon==null){
            streamCon = con;
        }else{
            System.out.println("PORRRRRRRRRRRRRRRRRAS");
            System.exit(1);
        }
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelPipeline cp = ctx.channel().pipeline();
        cp.addLast(QuicStructuredMessageEncoder.HANDLER_NAME,new QuicStructuredMessageEncoder());
        cp.addLast(QuicDelimitedMessageDecoder.HANDLER_NAME,new QuicDelimitedMessageDecoder(consumer,inConnection,customId));
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        consumer.streamInactiveHandler((QuicStreamChannel) ctx.channel(),customId);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        consumer.streamErrorHandler((QuicStreamChannel) ctx.channel(),cause,customId);
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                // Handle idle timeout event
                System.out.println("Idle timeout has occurred.");
                //ctx.close();
            }
        }
    }
}