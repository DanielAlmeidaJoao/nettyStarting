package tcpSupport.tcpChannelAPI.utils;

import java.net.InetSocketAddress;
import java.util.List;

public class TCPConnectingObject<T> {
    public final String customId;
    public final InetSocketAddress dest;
    public final List<T> pendingMessages;
    public final short streamProto;

    public TCPConnectingObject(String customId, InetSocketAddress dest, List<T> pendingMessages, short sourceProto) {
        this.customId = customId;
        this.dest = dest;
        this.pendingMessages = pendingMessages;
        this.streamProto = sourceProto;
    }
}
