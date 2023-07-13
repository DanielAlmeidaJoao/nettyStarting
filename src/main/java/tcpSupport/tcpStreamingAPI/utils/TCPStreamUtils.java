package tcpSupport.tcpStreamingAPI.utils;

import com.google.gson.Gson;
import quicSupport.utils.QUICLogics;
import tcpSupport.tcpStreamingAPI.channel.StreamingChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPStreamUtils {

    public static final Gson g = new Gson();
    public static final AtomicInteger channelIdCounter = new AtomicInteger();

    public static final String AUTO_CONNECT_ON_SEND_PROP = "AUTO_CONNECT";
    public static final String CUSTOM_ID_KEY = "CON_ID";

    public static final String READ_STREAM_PERIOD_KEY = "READ_STREAM_PERIOD_KEY";

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
        //channelProps.setProperty(QUICLogics.QUIC_METRICS,"true");
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_FILE_KEY,"keystore.jks");
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_PASSWORD_KEY,"simple");
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_ALIAS_KEY,"quicTestCert");

        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_FILE_KEY,"keystore2.jks");
        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_PASSWORD_KEY,"simple");
        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_ALIAS_KEY,"clientcert");
        channelProps.setProperty(QUICLogics.CONNECT_ON_SEND,"true");
        channelProps.setProperty(QUICLogics.MAX_IDLE_TIMEOUT_IN_SECONDS,"300");
        return channelProps;
    }

    public static Properties tcpChannelProperties(String address, String port){
        Properties channelProps = new Properties();
        channelProps.setProperty(StreamingChannel.ADDRESS_KEY,address);
        channelProps.setProperty(StreamingChannel.PORT_KEY,port);
        channelProps.setProperty(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
        //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"TRUE");
        return channelProps;
    }

}
