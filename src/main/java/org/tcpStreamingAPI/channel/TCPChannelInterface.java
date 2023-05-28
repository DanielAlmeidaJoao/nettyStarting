package org.tcpStreamingAPI.channel;

import quicSupport.utils.enums.ConnectionOrStreamType;

import java.net.InetSocketAddress;

public interface TCPChannelInterface {

    void openConnection(InetSocketAddress peer, ConnectionOrStreamType type);
    void closeConnection(InetSocketAddress peer);
    void closeServerSocket();
    void send(byte[] message, int len, InetSocketAddress peer, ConnectionOrStreamType structuredMessage);

    boolean isConnected(InetSocketAddress peer);
    InetSocketAddress [] getConnections();
    int connectedPeers();

    void shutDown();
}
