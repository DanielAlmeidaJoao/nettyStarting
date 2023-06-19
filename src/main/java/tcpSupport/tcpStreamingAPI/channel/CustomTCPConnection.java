package tcpSupport.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public class CustomTCPConnection {
    public final Channel channel;
    public final TransmissionType type;
    public final InetSocketAddress host;
    public final String conId;
    public CustomTCPConnection(Channel channel, TransmissionType type, InetSocketAddress listeningAddress, String conId){
        this.channel=channel;
        this.type=type;
        this.host = listeningAddress;
        this.conId = conId;
    }
}
