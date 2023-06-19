package tcpSupport.tcpStreamingAPI.utils;

import com.google.gson.Gson;

import java.util.concurrent.atomic.AtomicInteger;

public class TCPStreamUtils {

    public static final Gson g = new Gson();
    public static final AtomicInteger channelIdCounter = new AtomicInteger();

    public static final String AUTO_CONNECT_ON_SEND_PROP = "AUTO_CONNECT";

}
