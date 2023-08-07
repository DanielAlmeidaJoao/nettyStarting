package tcpSupport.tcpChannelAPI.utils;

import java.net.InetSocketAddress;
import java.util.List;

public class TCPConnectingObject<T> {
    public final String customId;
    public final InetSocketAddress dest;
    public final List<T> pendingMessages;

    public TCPConnectingObject(String customId,InetSocketAddress dest, List<T> pendingMessages) {
        this.customId = customId;
        this.dest = dest;
        this.pendingMessages = pendingMessages;
    }
}
