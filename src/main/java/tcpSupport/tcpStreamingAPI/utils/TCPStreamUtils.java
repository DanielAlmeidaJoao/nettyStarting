package tcpSupport.tcpStreamingAPI.utils;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.Pair;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class TCPStreamUtils {

    public static final Gson g = new Gson();

    public static final String AUTO_CONNECT_ON_SEND_PROP = "AUTO_CONNECT";
    public static boolean isConnecting(InetSocketAddress peer,
                                                  Map<String, Pair<InetSocketAddress, List<Pair<byte[], Integer>>>> connecting){
        for (Pair<InetSocketAddress, List<Pair<byte[], Integer>>> value : connecting.values()) {
            if(value.getKey().equals(peer)){
                return true;
            }
        }
        return false;
    }
}
