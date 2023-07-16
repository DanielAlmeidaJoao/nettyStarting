package tcpSupport.tcpChannelAPI.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.net.InetSocketAddress;
import java.util.List;

public class TCPConnectingObject {
    public final String customId;
    public final InetSocketAddress dest;
    public final List<Pair<byte [],Integer>> pendingMessages;

    public TCPConnectingObject(String customId,InetSocketAddress dest, List<Pair<byte[], Integer>> pendingMessages) {
        this.customId = customId;
        this.dest = dest;
        this.pendingMessages = pendingMessages;
    }
}
