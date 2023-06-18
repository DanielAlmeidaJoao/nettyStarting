package tcpSupport.tcpStreamingAPI.channel;

import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

public interface TCPChannelInterface {

    String openConnection(InetSocketAddress peer, TransmissionType type);
    void closeConnection(InetSocketAddress peer);
    void closeServerSocket();
    void send(byte[] message, int len, InetSocketAddress peer, TransmissionType unstructured);

    boolean isConnected(InetSocketAddress peer);
    InetSocketAddress [] getConnections();
    int connectedPeers();

    void shutDown();

    TransmissionType getConnectionType(InetSocketAddress toInetSOcketAddress) throws NoSuchElementException;
}
