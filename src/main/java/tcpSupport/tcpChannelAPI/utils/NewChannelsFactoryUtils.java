package tcpSupport.tcpChannelAPI.utils;

import com.google.gson.Gson;
import io.netty.channel.Channel;
import quicSupport.utils.QUICLogics;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NewChannelsFactoryUtils {

    public static final Gson g = new Gson();
    public static final AtomicInteger channelIdCounter = new AtomicInteger();

    public static final String AUTO_CONNECT_ON_SEND_PROP = "autoConnect";
    public static final String CUSTOM_ID_KEY = "CON_ID";
    public static final String DEST_STREAM_PROTO = "STREAM_PROTO";


    public static final String READ_STREAM_PERIOD_KEY = "readInputStreamPeriod";
    public static final String CONNECT_TIMEOUT_MILLIS = "connectTimeout";

    public static final String CHANNEL_METRICS = "metrics";

    public final static String METRICS_INTERVAL_KEY = "metricsInterval";

    public static final String BUFF_ALOC_SIZE = "rcvBuffAlocSize";
    public static final String useBossThreadTCP = "tcpServerBossThread";

    public final static String SINGLE_THREADED_PROP="singleThreaded";
    public final static String SERVER_THREADS = "serverThreads";
    public final static String CLIENT_THREADS = "clientThreads";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String CHUNK_SIZE = "chunkSize";
    public final static String USE_BABEL_THREAD_TO_SEND = "useBabelThreadToSendM";

    public final static String TCP_IDLE_CONNECTION_TIMEOUT="TCPIdleConnectionTimeout";


    public static  <E, T> Map<E,T> getMapInst(boolean singleT){
        if(singleT){
            return new HashMap<>();
        }else{
            return new ConcurrentHashMap<>();
        }
    }

    public static Properties quicChannelProperty(String address, String port){
        Properties channelProps = new Properties();
        channelProps.setProperty(NewChannelsFactoryUtils.ADDRESS_KEY,address);
        channelProps.setProperty(NewChannelsFactoryUtils.PORT_KEY,port);
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_FILE_KEY,"keystore.jks");
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_PASSWORD_KEY,"simple");
        channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_ALIAS_KEY,"quicTestCert");

        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_FILE_KEY,"keystore2.jks");
        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_PASSWORD_KEY,"simple");
        channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_ALIAS_KEY,"clientcert");
        //channelProps.setProperty(QUICLogics.CONNECT_ON_SEND,"true");
        channelProps.setProperty(QUICLogics.MAX_IDLE_TIMEOUT_IN_SECONDS,"60");
        //channelProps.setProperty(QUICLogics.CongestionControlAlgorithm,"RENO");

        //channelProps.setProperty(NewChannelsFactoryUtils.SINGLE_CON_PER_PEER,"TRUE");
        channelProps.setProperty(QUICLogics.MAX_ACK_DELAY,"3000");
        //channelProps.setProperty(NewChannelsFactoryUtils.BUFF_ALOC_SIZE,(16*1024)+"");
        //channelProps.setProperty(QUICLogics.MAX_UDP_RCV_PAYLOD_SIZE,"65527");
        channelProps.setProperty(QUICLogics.INITIAL_MAX_DATA,(1024*64*10)+"");
        channelProps.setProperty(QUICLogics.INITIAL_MAX_STREAM_DATA_BIDIRECTIONAL_LOCAL,(1024*64)+"");
        channelProps.setProperty(QUICLogics.INITIAL_MAX_STREAM_DATA_BIDIRECTIONAL_REMOTE,(1024*64)+"");

        //channelProps.setProperty(NewChannelsFactoryUtils.CHANNEL_METRICS,"ON");
        //channelProps.setProperty(NewChannelsFactoryUtils.METRICS_INTERVAL_KEY,"30");

        //channelProps.setProperty(NewChannelsFactoryUtils.CHANNEL_METRICS,"ON");
        //channelProps.setProperty(QUICLogics.idleTimeoutPercentageHB,"12");
        //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"as");
        //channelProps.setProperty(QUICLogics.MAX_IDLE_TIMEOUT_IN_SECONDS,"20");

        return channelProps;
    }

    public static Properties tcpChannelProperties(String address, String port){
        Properties channelProps = new Properties();
        channelProps.setProperty(NewChannelsFactoryUtils.ADDRESS_KEY,address);
        channelProps.setProperty(NewChannelsFactoryUtils.PORT_KEY,port);
        //channelProps.setProperty(NewChannelsFactoryUtils.SINGLE_CON_PER_PEER,"TRUE");
        //channelProps.setProperty(NewChannelsFactoryUtils.CHANNEL_METRICS,"ON");
        //channelProps.setProperty(NewChannelsFactoryUtils.METRICS_INTERVAL_KEY,"30");

        //channelProps.setProperty(NettyTCPChannel.NOT_ZERO_COPY,"TRUE");

        //channelProps.setProperty(NewChannelsFactoryUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
        //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"TRUE");
        return channelProps;
    }
    public static Properties udpChannelProperties(String address, String port){
        Properties properties = new Properties();
        properties.setProperty(NewChannelsFactoryUtils.ADDRESS_KEY,address);
        properties.setProperty(NewChannelsFactoryUtils.PORT_KEY,port);
        properties.setProperty(udpSupport.client_server.NettyUDPServer.MIN_UDP_RETRANSMISSION_TIMEOUT,"200");
        properties.setProperty(udpSupport.client_server.NettyUDPServer.MAX_UDP_RETRANSMISSION_TIMEOUT,"100");

        properties.setProperty(udpSupport.client_server.NettyUDPServer.MAX_SEND_RETRIES_KEY,"200");
        //properties.setProperty(NettyUDPServer.UDP_BROADCAST_PROP,"20");
        //properties.setProperty(NewChannelsFactoryUtils.CHANNEL_METRICS,"ON");
        //properties.setProperty(NewChannelsFactoryUtils.METRICS_INTERVAL_KEY,"30");
        //properties.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"as");
        //properties.setProperty(udpSupport.client_server.NettyUDPServer.UDP_BROADCAST_PROP,"10");
        //properties.setProperty(UDPChannel.UDP_METRICS,"10");

        return properties;
    }

    public static void closeOnError(Channel channel){
        channel.disconnect();
        channel.close();
    }



    public static int serverThreads(Properties properties){
        return Integer.parseInt((String) properties.getOrDefault(SERVER_THREADS,"0"));
    }
    public static int clientThreads(Properties properties){
        return Integer.parseInt((String) properties.getOrDefault(CLIENT_THREADS,"1"));
    }
}
