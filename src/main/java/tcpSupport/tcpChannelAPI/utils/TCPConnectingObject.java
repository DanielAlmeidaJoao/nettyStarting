package tcpSupport.tcpChannelAPI.utils;

import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;
import java.util.List;

public class TCPConnectingObject {
    public final String customId;
    public final InetSocketAddress dest;
    public final List<ByteBuf> pendingMessages;

    public TCPConnectingObject(String customId,InetSocketAddress dest, List<ByteBuf> pendingMessages) {
        this.customId = customId;
        this.dest = dest;
        this.pendingMessages = pendingMessages;
    }
}
