package tcpSupport.tcpChannelAPI.connectionSetups;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public class DummyClient implements ClientInterface{
    private static final Logger logger = LogManager.getLogger(DummyClient.class);

    @Override
    public void connect(InetSocketAddress peer, TransmissionType type, String conId) throws Exception {
        logger.warn("*connect* OPERATION NOT SUPPORTED ON <SERVER CHANNELS>!");
    }

    @Override
    public void shutDown() {}
}
