package pt.unl.fct.di.novasys.babel.channels;

import quicSupport.utils.enums.ConnectionOrStreamType;

import java.net.InetSocketAddress;

public interface NewIChannel<T> {
    void sendMessage(T var1, Host var2, short protoId);
    void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto, short handlerId);
    //exclusivelly for QUIC
    void sendMessage(T msg,String streamId,short proto);
    void sendMessage(byte[] data,int dataLen, String streamId, short sourceProto, short destProto, short handlerId);
    void sendStream(byte [] stream,int len,String streamId,short proto);
    void sendStream(byte [] stream,int len,Host host,short proto);


    void openConnection(Host var1, short protoId, ConnectionOrStreamType type);

    void registerChannelInterest(short protoId);



    void createStream(Host peer);

    /**
     * removes 'proto' from the set of the protocols using this streamId.
     * The stream is closed if the set becomes empty or if proto is a negative number
     * @param streamId
     * @param proto
     */
    void closeStream(String streamId, short proto);

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
}
