package tcpSupport.tcpChannelAPI.connectionSetups;

import io.netty.channel.EventLoopGroup;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public interface ClientInterface {

    void connect(InetSocketAddress peer, TransmissionType type, String conId, short destProto) throws Exception;
    void shutDown();

    EventLoopGroup getEventLoopGroup();

}
