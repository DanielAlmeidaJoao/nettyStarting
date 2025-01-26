package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.UDPNettyImplementations;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.NetworkRole;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.metrics.UDPNetworkStatsWrapper;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.utils.funcs.OnReadMetricsFunc;

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
