package udpSupport.channels;

import quicSupport.utils.enums.NetworkRole;
import udpSupport.metrics.UDPNetworkStatsWrapper;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.net.InetSocketAddress;
import java.util.List;

public interface UDPChannelInterface<T> {


    void shutDownServerClient();

    boolean metricsEnabled();

    void sendMessage(T message, InetSocketAddress dest);

    InetSocketAddress getSelf();

    void readMetrics(OnReadMetricsFunc onReadMetricsFunc);

    NetworkRole getNetworkRole();
    List<UDPNetworkStatsWrapper> getMetrics();


}
