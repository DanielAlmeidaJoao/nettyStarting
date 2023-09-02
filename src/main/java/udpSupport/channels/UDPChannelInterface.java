package udpSupport.channels;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.NetworkRole;
import udpSupport.metrics.UDPNetworkStatsWrapper;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.net.InetSocketAddress;
import java.util.List;

public interface UDPChannelInterface {


    void shutDownServerClient();

    boolean metricsEnabled();

    void sendMessage(BabelMessage message, InetSocketAddress dest);

    InetSocketAddress getSelf();

    void readMetrics(OnReadMetricsFunc onReadMetricsFunc);

    NetworkRole getNetworkRole();
    List<UDPNetworkStatsWrapper> getMetrics();


}
