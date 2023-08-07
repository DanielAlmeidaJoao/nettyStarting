package udpSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.DefaultEventExecutor;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class SingleThreadedUDPChannel<T> extends UDPChannel<T> {
    private final DefaultEventExecutor executor;

    public SingleThreadedUDPChannel(Properties properties, UDPChannelHandlerMethods udpChannelHandlerMethods, BabelMessageSerializerInterface<T> serializer) throws IOException {
        super(properties, true,udpChannelHandlerMethods,serializer);
        executor = new DefaultEventExecutor();
    }

    @Override
    public void sendMessage(T message, InetSocketAddress dest) {
        executor.execute(() -> super.sendMessage(message, dest));
    }

    @Override
    public void deliverMessage(ByteBuf message, InetSocketAddress from) {
        executor.execute(() -> super.deliverMessage(message, from));
    }

    @Override
    public void messageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest) {
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
