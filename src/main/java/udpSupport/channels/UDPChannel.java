package udpSupport.channels;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import udpSupport.client_server.NettyUDPServer;
import udpSupport.metrics.ChannelStats;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;

public abstract class UDPChannel implements UDPChannelConsumer{
    private static final Logger logger = LogManager.getLogger(UDPChannel.class);
    public final static String NAME = "QUIC_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8575";
    private NettyUDPServer udpServer;
    private ChannelStats metrics;
    private final InetSocketAddress self;

    public UDPChannel(Properties properties, boolean singleThreaded) throws Exception {
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        if( properties.containsKey("metrics")){
            metrics = new ChannelStats();
        }
        udpServer=new NettyUDPServer(this,metrics,self);
    }

    public void sendMessage(byte [] message, InetSocketAddress dest,int len){
        udpServer.sendMessage(message,dest,len);
    }

    @Override
    public void deliverMessage(byte[] message, InetSocketAddress from){
        onDeliverMessage(message,from);
    }
    public abstract void onDeliverMessage(byte[] message, InetSocketAddress from);
    @Override
    public void messageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest){
        onMessageSentHandler(success,error,message,dest);
    }
    public abstract void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest);

}
