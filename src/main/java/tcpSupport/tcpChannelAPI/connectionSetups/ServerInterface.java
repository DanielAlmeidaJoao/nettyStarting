package tcpSupport.tcpChannelAPI.connectionSetups;

import io.netty.channel.EventLoopGroup;

public interface ServerInterface {

    void startServer() throws Exception;
    void shutDown();

    EventLoopGroup getEventLoopGroup();

}
