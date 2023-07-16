package tcpSupport.tcpChannelAPI.channel;

import io.netty.channel.Channel;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;

import java.net.InetSocketAddress;

public class CustomTCPConnection {
    public final Channel channel;
    public final TransmissionType type;
    public final InetSocketAddress host;
    public final String conId;
    public final boolean inConnection;
    public final BabelInputStream inputStream;
    public CustomTCPConnection(Channel channel, TransmissionType type, InetSocketAddress listeningAddress, String conId, boolean inConnection, BabelInputStream babelInputStream){
        this.channel=channel;
        this.type=type;
        this.host = listeningAddress;
        this.conId = conId;
        this.inConnection=inConnection;
        this.inputStream = babelInputStream;
    }

    public void close(){
        channel.disconnect();
        channel.close();
    }
}
