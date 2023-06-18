package quicSupport.utils;

import java.net.InetSocketAddress;

public class ConnectionId {
    public final InetSocketAddress address;
    public final String linkId;

    private ConnectionId(InetSocketAddress address, String linkId){
        this.address = address;
        this.linkId = linkId;
    }

    public static ConnectionId of(InetSocketAddress address, String linkId){
        return new ConnectionId(address,linkId);
    }
}
