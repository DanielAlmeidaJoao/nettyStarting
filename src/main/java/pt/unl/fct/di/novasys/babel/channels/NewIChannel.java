package pt.unl.fct.di.novasys.babel.channels;

import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

public interface NewIChannel<T> {
    String openConnection(Host var1, short protoId, TransmissionType type);
    //void createStream(Host peer, TransmissionType type, short sourceProto, short destProto, short handlerId);
    void sendMessage(T var1, Host var2, short protoId);
    void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto, short handlerId);
    //exclusivelly for QUIC
    void sendMessage(T msg,String linkId,short proto);
    void sendMessage(byte[] data,int dataLen, String linkId, short sourceProto, short destProto, short handlerId);
    void sendStream(byte [] stream,int len,String linkId,short proto);
    void sendStream(byte [] stream,int len,Host host,short proto);
    TransmissionType getConnectionTransmissionType(Host host)  throws NoSuchElementException;
    TransmissionType getConnectionStreamTransmissionType(String linkId)  throws NoSuchElementException;
    void registerChannelInterest(short protoId);
    /**
     * removes 'proto' from the set of the protocols using this streamId.
     * The stream is closed if the set becomes empty or if proto is a negative number
     * @param linkId
     * @param proto
     */
    void closeLink(String linkId, short proto);
    /**
     * removes 'protoId' from the set of the protocols using the connection 'peer'.
     * The connection is closed if the set becomes empty or if protoId is a negative number
     * @param peer
     * @param protoId
     */
    void closeConnection(Host peer, short protoId);
    boolean isConnected(Host peer);
    String [] getStreams();
    InetSocketAddress [] getConnections();
    int connectedPeers();
    boolean shutDownChannel(short protoId);
    short getChannelProto();
    NetworkProtocol getNetWorkProtocol();
}
