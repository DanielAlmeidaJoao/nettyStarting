package tcpSupport.tcpStreamingAPI.utils;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
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

}
