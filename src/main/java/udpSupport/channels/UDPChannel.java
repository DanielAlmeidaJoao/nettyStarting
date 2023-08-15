package udpSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import quicSupport.utils.enums.NetworkRole;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;
import udpSupport.client_server.NettyUDPServer;
import udpSupport.metrics.ChannelStats;
import udpSupport.metrics.UDPNetworkStatsWrapper;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

public class UDPChannel<T> implements UDPChannelConsumer<T>,UDPChannelInterface<T>{
    private static final Logger logger = LogManager.getLogger(UDPChannel.class);
    public final static String NAME = "UDP_CHANNEL";

    public final static String ADDRESS_KEY = "UDP_address";
    public final static String PORT_KEY = "UDP_port";

    public final static String DEFAULT_PORT = "8578";
    private final NettyUDPServer udpServer;
    private ChannelStats metrics;
    private final InetSocketAddress self;
    private final UDPChannelHandlerMethods channelHandlerMethods;
    private final BabelMessageSerializerInterface<T> serializer;

    public UDPChannel(Properties properties, boolean singleThreaded, UDPChannelHandlerMethods consumer, BabelMessageSerializerInterface<T> serializer) throws IOException {
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
            //addr = Inet4Address.getByName("0.0.0.0");
        else
            throw new IllegalArgumentException(NAME + " requires binding address");
        this.serializer = serializer;
        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        if( properties.getProperty(TCPChannelUtils.CHANNEL_METRICS)!=null){
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
    public void sendMessage(T message, InetSocketAddress dest){
        ByteBuf buf = Unpooled.buffer();
        try{
            serializer.serialize(message,buf);
            udpServer.sendMessage(buf,dest);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public InetSocketAddress getSelf(){
        return self;
    }
    public void peerDown(InetSocketAddress peer){
        channelHandlerMethods.onPeerDown(peer);
    }


    @Override
    public void deliverMessage(ByteBuf message, InetSocketAddress from){
        try {
            T babelMessage = serializer.deserialize(message);
            channelHandlerMethods.onDeliverMessage(babelMessage,from);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void messageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest){
        channelHandlerMethods.onMessageSentHandler(success,error,message,dest);
    }

    public void readMetrics(OnReadMetricsFunc onReadMetricsFunc){
        if(metricsEnabled()){
            onReadMetricsFunc.execute(metrics.cloneChannelMetric());
        }else{
            logger.info("METRICS NOT ENABLED. ADD PROPERTY 'metrics=true'");
        }
    }

    public List<UDPNetworkStatsWrapper> getMetrics(){
        return metrics == null ? null : metrics.cloneChannelMetric();
    }
    @Override
    public NetworkRole getNetworkRole() {
        return NetworkRole.P2P_CHANNEL;
    }


}
