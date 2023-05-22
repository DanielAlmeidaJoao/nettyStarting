package udpSupport.channels;

import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.net.InetSocketAddress;

public interface UDPChannelInterface {


    void shutDownServerClient();

    boolean metricsEnabled();

    void sendMessage(byte[] message, InetSocketAddress dest, int len);

    InetSocketAddress getSelf();

    void readMetrics(OnReadMetricsFunc onReadMetricsFunc);

}
