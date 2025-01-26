package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.QuicNettyImplementations;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.NetworkProtocol;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.NetworkRole;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.handlerFunctions.ReadMetricsHandler;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.metrics.ConnectionProtocolMetrics;

import java.net.InetSocketAddress;
import java.util.List;

public interface NettyChannelInterface extends SendBytesInterface {

    String open(InetSocketAddress peer, TransmissionType type, short sourceProto, short destProto, boolean always);

    void closeConnection(InetSocketAddress peer);

    void closeLink(String streamId);

    void readMetrics(ReadMetricsHandler handler);


    void send(InetSocketAddress peer, BabelMessage message);

    boolean enabledMetrics();

    boolean isConnected(InetSocketAddress peer);
    String [] getStreams();
    InetSocketAddress [] getAddressToQUICCons();
    int connectedPeers();

    void shutDown();

    TransmissionType getConnectionType(String streamId);

    boolean isConnected(String connectionID);

    NetworkRole getNetworkRole();

    List<ConnectionProtocolMetrics> currentMetrics();

    List<ConnectionProtocolMetrics> oldMetrics();

    NetworkProtocol getNetworkProtocol();
}
