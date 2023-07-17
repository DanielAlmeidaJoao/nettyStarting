package quicSupport.channels;

import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.handlerFunctions.ReadMetricsHandler;

import java.net.InetSocketAddress;

public interface NettyChannelInterface extends SendBytesInterface {

    String open(InetSocketAddress peer, TransmissionType type);

    void closeConnection(InetSocketAddress peer);

    void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler);

    void closeLink(String streamId);

    void readMetrics(ReadMetricsHandler handler);


    void send(InetSocketAddress peer, byte[] message, int len);

    boolean enabledMetrics();

    boolean isConnected(InetSocketAddress peer);
    String [] getStreams();
    InetSocketAddress [] getAddressToQUICCons();
    int connectedPeers();

    void shutDown();

    TransmissionType getConnectionType(InetSocketAddress toInetSOcketAddress);

    TransmissionType getConnectionType(String streamId);
    }
