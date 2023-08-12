package tcpSupport.tcpChannelAPI.connectionSetups;

import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface ClientInterface {

    void connect(InetSocketAddress peer, TransmissionType type, String conId) throws Exception;
    void shutDown();
}
