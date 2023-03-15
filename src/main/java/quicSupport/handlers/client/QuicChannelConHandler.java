package quicSupport.handlers.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;

public class QuicChannelConHandler extends ChannelInboundHandlerAdapter {
    private InNettyChannelListener listener;

    public QuicChannelConHandler(InNettyChannelListener listener) {
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // As we did not allow any remote initiated streams we will never see this method called.
        // That said just let us keep it here to demonstrate that this handle would be called
        // for each remote initiated stream.
        System.out.println("UNCALLED WAS CALLED!!!");
        ctx.close();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
