package tcpSupport.tcpStreamingAPI.channel;

import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

public interface TCPChannelInterface {

    String openConnection(InetSocketAddress peer, TransmissionType type);
    void closeConnection(InetSocketAddress peer);
    void closeConnection(String connectionId);
    void closeServerSocket();
    void send(byte[] message, int len, InetSocketAddress peer, TransmissionType unstructured);
    void send(byte[] message, int len, String conId, TransmissionType unstructured);


    boolean isConnected(InetSocketAddress peer);
    InetSocketAddress [] getNettyIdToConnection();
    int connectedPeers();

    void shutDown();

    TransmissionType getConnectionType(InetSocketAddress toInetSOcketAddress) throws NoSuchElementException;
    TransmissionType getConnectionStreamTransmissionType(String streamId)  throws NoSuchElementException;


    }
