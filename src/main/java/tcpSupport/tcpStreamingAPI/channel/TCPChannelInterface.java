package tcpSupport.tcpStreamingAPI.channel;

import quicSupport.channels.SendBytesInterface;
import quicSupport.utils.enums.TransmissionType;

import java.io.InputStream;
import java.net.InetSocketAddress;

public interface TCPChannelInterface extends SendBytesInterface {

    String openConnection(InetSocketAddress peer, TransmissionType type);
    void closeConnection(InetSocketAddress peer);
    void closeConnection(String connectionId);
    void closeServerSocket();
    void send(InetSocketAddress peer, byte[] message, int len, TransmissionType type);
    void send(String streamId, byte[] message,int len, TransmissionType type);


    void sendInputStream(InputStream inputStream, int len, InetSocketAddress peer, String conId);

    boolean isConnected(InetSocketAddress peer);
    InetSocketAddress [] getNettyIdToConnection();
    int connectedPeers();

    String [] getLinks();

    void shutDown();

    TransmissionType getConnectionType(InetSocketAddress toInetSOcketAddress);
    TransmissionType getConnectionStreamTransmissionType(String streamId);


    }
