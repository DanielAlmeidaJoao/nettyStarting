package quicSupport.channels;

import org.apache.commons.lang3.tuple.Triple;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface CustomQuicChannelInterface {

    String open(InetSocketAddress peer, TransmissionType type);

    void closeConnection(InetSocketAddress peer);

    void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler);

    String createStream(InetSocketAddress peer, TransmissionType type, Triple<Short,Short,Short> args);

    void closeStream(String streamId);

    void readMetrics(QuicReadMetricsHandler handler);

    void send(String streamId, byte[] message, int len, TransmissionType type);

    void send(InetSocketAddress peer, byte[] message, int len, TransmissionType type);

    boolean enabledMetrics();

    boolean isConnected(InetSocketAddress peer);
    String [] getStreams();
    InetSocketAddress [] getConnections();
    int connectedPeers();

    void shutDown();

    TransmissionType getConnectionType(InetSocketAddress toInetSOcketAddress);

    TransmissionType getConnectionType(String streamId);
}
