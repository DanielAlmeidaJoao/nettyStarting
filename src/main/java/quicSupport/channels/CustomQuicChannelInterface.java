package quicSupport.channels;

import org.apache.commons.lang3.tuple.Triple;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.net.InetSocketAddress;

public interface CustomQuicChannelInterface {

    void open(InetSocketAddress peer, ConnectionOrStreamType type);

    void closeConnection(InetSocketAddress peer);

    void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler);

    void createStream(InetSocketAddress peer, ConnectionOrStreamType type, Triple<Short,Short,Short> args);

    void closeStream(String streamId);

    void readMetrics(QuicReadMetricsHandler handler);

    void send(String streamId, byte[] message, int len,ConnectionOrStreamType type);

    void send(InetSocketAddress peer, byte[] message, int len,ConnectionOrStreamType type);

    boolean enabledMetrics();

    boolean isConnected(InetSocketAddress peer);
    String [] getStreams();
    InetSocketAddress [] getConnections();
    int connectedPeers();

    void shutDown();

    ConnectionOrStreamType getConnectionType(InetSocketAddress toInetSOcketAddress);

    ConnectionOrStreamType getConnectionType(String streamId);
}
