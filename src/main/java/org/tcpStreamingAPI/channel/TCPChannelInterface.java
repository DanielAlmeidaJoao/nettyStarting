package org.tcpStreamingAPI.channel;

import java.net.InetSocketAddress;

public interface TCPChannelInterface {

    void openConnection(InetSocketAddress peer);
    void closeConnection(InetSocketAddress peer);
    void closeServerSocket();
    void send(byte[] message, int len,InetSocketAddress peer);

    boolean isConnected(InetSocketAddress peer);
    InetSocketAddress [] getConnections();
    int connectedPeers();
}
