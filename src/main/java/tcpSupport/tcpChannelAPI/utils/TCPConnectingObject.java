package tcpSupport.tcpChannelAPI.utils;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.net.InetSocketAddress;
import java.util.List;

public class TCPConnectingObject {
    public final String customId;
    public final InetSocketAddress dest;
    public final List<BabelMessage> pendingMessages;
    public final short streamProto;

    public TCPConnectingObject(String customId, InetSocketAddress dest, List<BabelMessage> pendingMessages, short sourceProto) {
        this.customId = customId;
        this.dest = dest;
        this.pendingMessages = pendingMessages;
        this.streamProto = sourceProto;
    }
}
