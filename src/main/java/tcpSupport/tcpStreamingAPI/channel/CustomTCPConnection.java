package tcpSupport.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public class CustomTCPConnection {
    public final Channel channel;
    public final TransmissionType type;
    public final InetSocketAddress host;
    public final String conId;
    public final boolean inConnection;
    public CustomTCPConnection(Channel channel, TransmissionType type, InetSocketAddress listeningAddress, String conId, boolean inConnection){
        this.channel=channel;
        this.type=type;
        this.host = listeningAddress;
        this.conId = conId;
        this.inConnection=inConnection;
    }

    public void close(){
        channel.disconnect();
        channel.close();
    }
}
