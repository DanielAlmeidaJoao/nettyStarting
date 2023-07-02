package pt.unl.fct.di.novasys.babel.channels;

import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.TransmissionType;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

public interface NewIChannel<T> {
    String openConnection(Host var1, short protoId, TransmissionType type);
    void sendMessage(T var1, Host var2, short protoId);
    void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto, short handlerId);
    void sendMessage(T msg,String linkId,short proto);
    void sendMessage(byte[] data,int dataLen, String streamId, short sourceProto, short destProto, short handlerId);
    void sendStream(byte [] stream,int len,String streamId,short proto);
    void sendStream(byte [] stream,int len,Host host,short proto);
    void sendStream(InputStream inputStream, int len, Host peer, short proto);
    void sendStream(InputStream inputStream, int len, String conId, short proto);
    void sendStream(InputStream inputStream, Host peer, short proto);
    void sendStream(InputStream inputStream, String conId, short proto);

    TransmissionType getTransmissionType(Host host)  throws NoSuchElementException;
    TransmissionType getTransmissionType(String streamId)  throws NoSuchElementException;
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
    String [] getLinks();
    InetSocketAddress [] getConnections();
    int connectedPeers();
    boolean shutDownChannel(short protoId);
    short getChannelProto();
    NetworkProtocol getNetWorkProtocol();
}
