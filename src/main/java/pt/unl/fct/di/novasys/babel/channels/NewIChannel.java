package pt.unl.fct.di.novasys.babel.channels;

import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetrics;
import udpSupport.metrics.UDPNetworkStatsWrapper;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.NoSuchElementException;

public interface NewIChannel<T> {
    String openMessageConnection(Host host, short protoId);
    String openStreamConnection(Host host, short protoId);

    void sendMessage(T message, Host host, short protoId);
    void sendMessage(T msg,String connectionID,short proto);

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

    List<ConnectionProtocolMetrics> currentMetrics();
    List<ConnectionProtocolMetrics> oldMetrics();
    List<UDPNetworkStatsWrapper> getUDPMetrics();

    NetworkProtocol getNetWorkProtocol();
    NetworkRole getNetworkRole();
}
