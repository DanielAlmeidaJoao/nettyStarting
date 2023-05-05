package udpSupport.channels;

import io.netty.util.concurrent.DefaultEventExecutor;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public abstract class SingleThreadedUDPChannel extends UDPChannel {
    private final DefaultEventExecutor executor;

    public SingleThreadedUDPChannel(Properties properties) throws IOException {
        super(properties, true);
        executor = new DefaultEventExecutor();
    }

    @Override
    public void sendMessage(byte[] message, InetSocketAddress dest, int len) {
        executor.execute(() -> super.sendMessage(message, dest, len));
    }

    @Override
    public void deliverMessage(byte[] message, InetSocketAddress from) {
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
    public void peerDown(InetSocketAddress peer) {
        executor.execute(() -> super.peerDown(peer));
    }
}
