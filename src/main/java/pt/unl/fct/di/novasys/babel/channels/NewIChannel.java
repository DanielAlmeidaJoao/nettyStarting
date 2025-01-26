package pt.unl.fct.di.novasys.babel.channels;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.NetworkProtocol;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.NetworkRole;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.metrics.ConnectionProtocolMetrics;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.metrics.UDPNetworkStatsWrapper;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.NoSuchElementException;

public interface NewIChannel {
    String openMessageConnection(Host host, short protoId, boolean always);
    String openStreamConnection(Host host, short protoId,short destProto, boolean always);

    void sendMessage(BabelMessage message, Host host, short protoId);
    void sendMessage(BabelMessage msg,String connectionID,short proto);

    void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto);
    void sendMessage(byte[] data,int dataLen, String connectionID, short sourceProto, short destProto);
    TransmissionType getConnectionType(String connectionId)  throws NoSuchElementException;
    void registerChannelInterest(short protoId);
    /**
     * removes 'proto' from the set of the protocols using this streamId.
     * The stream is closed if the set becomes empty or if proto is a negative number
     * @param connectionID
     * @param proto
     */
    void closeConnection(String connectionID, short proto);
    /**
     * removes 'protoId' from the set of the protocols using the connection 'peer'.
     * The connection is closed if the set becomes empty or if protoId is a negative number
     * @param peer
     * @param protoId
     */
    void closeConnection(Host peer, short protoId);
    boolean isConnected(Host peer);

    boolean isConnected(String connectionID);

    String [] getConnectionsIds();
    InetSocketAddress [] getConnections();
    int connectedPeers();
    boolean shutDownChannel(short protoId);
    short getChannelProto();

    List<ConnectionProtocolMetrics> activeConnectionsMetrics();
    List<ConnectionProtocolMetrics> closedConnectionsMetrics();
    List<UDPNetworkStatsWrapper> getUDPMetrics();

    NetworkProtocol getNetWorkProtocol();
    NetworkRole getNetworkRole();
}
