package tcpSupport.tcpChannelAPI.connectionSetups;

import io.netty.channel.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DummyServer implements ServerInterface{
    private static final Logger logger = LogManager.getLogger(DummyServer.class);

    @Override
    public void startServer() throws Exception {
        logger.warn("*startServer* OPERATION NOT SUPPORTED ON <CLIENT CHANNELS>!");

    }

    @Override
    public void shutDown() {}

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return null;
    }
}
