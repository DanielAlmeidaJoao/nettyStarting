package quicSupport.channels;

import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.handlerFunctions.ReadMetricsHandler;

import java.net.InetSocketAddress;

public interface NettyChannelInterface<T> extends SendBytesInterface<T> {

    String open(InetSocketAddress peer, TransmissionType type);

    void closeConnection(InetSocketAddress peer);

    void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler);

    void closeLink(String streamId);

    void readMetrics(ReadMetricsHandler handler);


    void send(InetSocketAddress peer, T message);

    boolean enabledMetrics();

    boolean isConnected(InetSocketAddress peer);
    String [] getStreams();
    InetSocketAddress [] getAddressToQUICCons();
    int connectedPeers();

    void shutDown();

    TransmissionType getConnectionType(String streamId);

    boolean isConnected(String connectionID);

    NetworkRole getNetworkRole();
}
