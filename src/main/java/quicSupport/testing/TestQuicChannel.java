package quicSupport.testing;

import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.CustomQuicChannel;
import quicSupport.client_server.QuicServerExample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class TestQuicChannel extends CustomQuicChannel {

    private static final Logger logger = LogManager.getLogger(TestQuicChannel.class);
    //private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(TestQuicChannel.class);


    public TestQuicChannel(Properties properties) throws IOException {
        super(properties);
    }

    @Override
    public void onChannelClosed(InetSocketAddress peer) {

    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from) {

    }

    @Override
    public void channelReadConfigData(String s, byte[] bytes) {

    }

    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, InetSocketAddress peer) {

    }

    @Override
    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {

    }

    public static void main(String args []) throws IOException {
        logger.info("STARREDD");
        Properties properties = new Properties();
        properties.setProperty("address","localhost");
        String port = args[0];
        properties.setProperty("port",port);
        TestQuicChannel testQuicChannel = new TestQuicChannel(properties);
        if("8081".equals(port)){
            InetSocketAddress remote = new InetSocketAddress("localhost",8082);
            testQuicChannel.openConnection(remote);
        }
    }
}
