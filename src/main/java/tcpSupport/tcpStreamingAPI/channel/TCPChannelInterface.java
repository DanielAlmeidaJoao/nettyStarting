package tcpSupport.tcpStreamingAPI.channel;

import quicSupport.utils.enums.TransmissionType;

import java.io.InputStream;
import java.net.InetSocketAddress;

public interface TCPChannelInterface {

    String openConnection(InetSocketAddress peer, TransmissionType type);
    void closeConnection(InetSocketAddress peer);
    void closeConnection(String connectionId);
    void closeServerSocket();
    boolean send(byte[] message, int len, InetSocketAddress peer, TransmissionType unstructured);
    boolean send(byte[] message, int len, String conId, TransmissionType unstructured);

    boolean sendInputStream(InputStream inputStream, int len, InetSocketAddress peer, String conId);

    boolean isConnected(InetSocketAddress peer);
    InetSocketAddress [] getNettyIdToConnection();
    int connectedPeers();

    String [] getLinks();

    void shutDown();

    TransmissionType getConnectionType(InetSocketAddress toInetSOcketAddress);
    TransmissionType getConnectionStreamTransmissionType(String streamId);


    }
