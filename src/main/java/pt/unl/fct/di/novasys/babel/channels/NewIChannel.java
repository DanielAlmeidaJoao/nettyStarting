package pt.unl.fct.di.novasys.babel.channels;

import java.net.InetSocketAddress;

public interface NewIChannel<T> {
    void sendMessage(T var1, Host var2, short protoId);



    void openConnection(Host var1, short protoId);

    void registerChannelInterest(short protoId);

    //exclusivelly for QUIC
    void sendMessage(T msg,String streamId,short proto);

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
