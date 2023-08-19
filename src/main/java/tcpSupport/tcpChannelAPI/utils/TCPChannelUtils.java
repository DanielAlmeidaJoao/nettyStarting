package tcpSupport.tcpChannelAPI.utils;

import com.google.gson.Gson;
import quicSupport.utils.QUICLogics;
import tcpSupport.tcpChannelAPI.channel.NettyTCPChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPChannelUtils {

    public static final Gson g = new Gson();
    public static final AtomicInteger channelIdCounter = new AtomicInteger();

    public static final String AUTO_CONNECT_ON_SEND_PROP = "AUTO_CONNECT";
    public static final String CUSTOM_ID_KEY = "CON_ID";

    public static final String READ_STREAM_PERIOD_KEY = "READ_STREAM_PERIOD_KEY";
    public static final String SINGLE_CON_PER_PEER = "SINGLE_PEER_CONNECTION";

    public static final String CONNECT_TIMEOUT_MILLIS = "CONNECT_TIMEOUT_MILLIS";

    public static final String CHANNEL_METRICS = "channel_metrics";

    public final static String METRICS_INTERVAL_KEY = "metrics_interval";


    public static  <E, T> Map<E,T> getMapInst(boolean singleT){
        if(singleT){
            return new HashMap<>();
        }else{
            return new ConcurrentHashMap<>();
        }
    }

    public static Properties quicChannelProperty(String address, String port){
        Properties channelProps = new Properties();
        channelProps.setProperty(QUICLogics.ADDRESS_KEY,address);
        channelProps.setProperty(QUICLogics.PORT_KEY,port);
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_FILE_KEY,"keystore.jks");
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_PASSWORD_KEY,"simple");
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_ALIAS_KEY,"quicTestCert");

        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_FILE_KEY,"keystore2.jks");
        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_PASSWORD_KEY,"simple");
        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_ALIAS_KEY,"clientcert");
        //channelProps.setProperty(QUICLogics.CONNECT_ON_SEND,"true");
        channelProps.setProperty(QUICLogics.MAX_IDLE_TIMEOUT_IN_SECONDS,"60");
        channelProps.setProperty(TCPChannelUtils.SINGLE_CON_PER_PEER,"TRUE");
        //channelProps.setProperty(TCPChannelUtils.CHANNEL_METRICS,"ON");
        //channelProps.setProperty(TCPChannelUtils.METRICS_INTERVAL_KEY,"30");

        //channelProps.setProperty(TCPChannelUtils.CHANNEL_METRICS,"ON");
        //channelProps.setProperty(QUICLogics.WITH_HEART_BEAT,"true");
        //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"as");

        return channelProps;
    }

    public static Properties tcpChannelProperties(String address, String port){
        Properties channelProps = new Properties();
        channelProps.setProperty(NettyTCPChannel.ADDRESS_KEY,address);
        channelProps.setProperty(NettyTCPChannel.PORT_KEY,port);
        channelProps.setProperty(TCPChannelUtils.SINGLE_CON_PER_PEER,"TRUE");
        //channelProps.setProperty(TCPChannelUtils.CHANNEL_METRICS,"ON");
        //channelProps.setProperty(TCPChannelUtils.METRICS_INTERVAL_KEY,"30");

        //channelProps.setProperty(NettyTCPChannel.ZERO_COPY,"TRUE");

        //channelProps.setProperty(TCPChannelUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
        //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"TRUE");
        return channelProps;
    }
    public static Properties udpChannelProperties(String address, String port){
        Properties properties = new Properties();
        properties.setProperty("UDP_address",address);
        //properties.setProperty("UDP_metrics","on");
        properties.setProperty("UDP_port",port);
        properties.setProperty(udpSupport.client_server.NettyUDPServer.UDP_RETRANSMISSION_TIMEOUT,"500");
        properties.setProperty(udpSupport.client_server.NettyUDPServer.MAX_SEND_RETRIES_KEY,"20");
        //properties.setProperty(TCPChannelUtils.CHANNEL_METRICS,"ON");
        //properties.setProperty(TCPChannelUtils.METRICS_INTERVAL_KEY,"30");
        //properties.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"as");
        //properties.setProperty(udpSupport.client_server.NettyUDPServer.UDP_BROADCAST_PROP,"10");
        //properties.setProperty(UDPChannel.UDP_METRICS,"10");

        return properties;
    }
}
