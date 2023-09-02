package udpSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.DefaultEventExecutor;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class SingleThreadedUDPChannel extends UDPChannel {
    private final DefaultEventExecutor executor;

    public SingleThreadedUDPChannel(Properties properties, UDPChannelHandlerMethods udpChannelHandlerMethods, BabelMessageSerializer serializer) throws IOException {
        super(properties, true,udpChannelHandlerMethods,serializer);
        executor = new DefaultEventExecutor();
    }

    @Override
    public void sendMessage(BabelMessage message, InetSocketAddress dest) {
        executor.execute(() -> super.sendMessage(message, dest));
    }

    @Override
    public void deliverMessage(ByteBuf message, InetSocketAddress from) {
        final ByteBuf copy = message.retainedDuplicate();
        executor.execute(() -> {
            super.deliverMessage(copy, from);
            copy.release();
        });
    }

    @Override
    public void messageSentHandler(boolean success, Throwable error, BabelMessage message, InetSocketAddress dest) {
        executor.execute(() -> super.messageSentHandler(success, error, message, dest));
    }

    @Override
    public void readMetrics(OnReadMetricsFunc onReadMetricsFunc) {
        executor.execute(() -> super.readMetrics(onReadMetricsFunc));
    }

    @Override
    public void shutDownServerClient(){
        executor.execute(() -> {
            super.shutDownServerClient();
            executor.shutdownGracefully().getNow();
        });
    }
    @Override
    public void peerDown(InetSocketAddress peer) {
        executor.execute(() -> super.peerDown(peer));
    }
}
