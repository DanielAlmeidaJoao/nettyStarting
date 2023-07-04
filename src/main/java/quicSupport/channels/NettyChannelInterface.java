package quicSupport.channels;

import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.enums.TransmissionType;

import java.io.InputStream;
import java.net.InetSocketAddress;

public interface NettyChannelInterface extends SendBytesInterface {

    String open(InetSocketAddress peer, TransmissionType type);

    void closeConnection(InetSocketAddress peer);

    void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler);

    void closeLink(String streamId);

    void readMetrics(QuicReadMetricsHandler handler);


    void send(InetSocketAddress peer, byte[] message, int len, TransmissionType type);

    boolean enabledMetrics();

    boolean isConnected(InetSocketAddress peer);
    String [] getStreams();
    InetSocketAddress [] getAddressToQUICCons();
    int connectedPeers();

    void shutDown();

    TransmissionType getConnectionType(InetSocketAddress toInetSOcketAddress);

    TransmissionType getConnectionType(String streamId);
    void sendInputStream(InputStream inputStream, int len, InetSocketAddress peer, String conId);
    }