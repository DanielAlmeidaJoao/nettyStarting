package udpSupport.channels;

import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import udpSupport.client_server.NettyUDPServer;
import udpSupport.metrics.ChannelStats;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;

public class UDPChannel implements UDPChannelConsumer,UDPChannelInterface{
    private static final Logger logger = LogManager.getLogger(UDPChannel.class);
    public final static String NAME = "UDP_CHANNEL";

    public final static String ADDRESS_KEY = "UDP_address";
    public final static String PORT_KEY = "UDP_port";

    public final static String DEFAULT_PORT = "8578";
    public static final String UDP_METRICS = "UDP_metrics";
    private final NettyUDPServer udpServer;
    private ChannelStats metrics;
    private final InetSocketAddress self;
    private final UDPChannelHandlerMethods channelHandlerMethods;

    public UDPChannel(Properties properties, boolean singleThreaded, UDPChannelHandlerMethods consumer) throws IOException {
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
            //addr = Inet4Address.getByName("0.0.0.0");
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        if( properties.getProperty(UDP_METRICS)!=null){
            metrics = new ChannelStats(singleThreaded);
        }
        udpServer=new NettyUDPServer(this,metrics,self,properties);
        channelHandlerMethods = consumer;
    }

    @Override
    public void shutDownServerClient() {
        udpServer.shutDownServerClient();
    }

    public boolean metricsEnabled(){
        return metrics!=null;
    }
    public void sendMessage(byte [] message, InetSocketAddress dest,int len){
        udpServer.sendMessage(message,dest,len);
    }
    public InetSocketAddress getSelf(){
        return self;
    }
    public void peerDown(InetSocketAddress peer){
        channelHandlerMethods.onPeerDown(peer);
    }


    @Override
    public void deliverMessage(ByteBuf message, InetSocketAddress from){
        channelHandlerMethods.onDeliverMessage(message,from);
    }
    @Override
    public void messageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest){
        channelHandlerMethods.onMessageSentHandler(success,error,message,dest);
    }

    public void readMetrics(OnReadMetricsFunc onReadMetricsFunc){
        System.out.println("SUPPER METRICS CALLED "+metrics.getStatsMap().size());
        if(metricsEnabled()){
            onReadMetricsFunc.execute(metrics.cloneChannelMetric());
        }else{
            logger.info("METRICS NOT ENABLED. ADD PROPERTY 'metrics=true'");
        }
    }


}
