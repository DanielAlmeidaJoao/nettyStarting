package udpSupport.channels;

import io.netty.buffer.ByteBuf;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.net.InetSocketAddress;

public interface UDPChannelInterface {


    void shutDownServerClient();

    boolean metricsEnabled();

    void sendMessage(ByteBuf message, InetSocketAddress dest);

    InetSocketAddress getSelf();

    void readMetrics(OnReadMetricsFunc onReadMetricsFunc);

}
