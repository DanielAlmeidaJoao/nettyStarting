package tcpSupport.tcpChannelAPI.connectionSetups.messages;

import lombok.Getter;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Getter
public class HandShakeMessage {

    private String hostName;
    private int port;
    //private final Map<String,Object> properties;
    public final TransmissionType type;
    public final short destProto;
    /**
    public HandShakeMessage(String hostName, int port, TransmissionType type) {
        //this.properties = new HashMap<>();
        this.hostName=hostName;
        this.port=port;
        this.type=type;
        this.destProto = -1;
    } **/
    public HandShakeMessage(InetSocketAddress host, TransmissionType type, short destProto) {
        //this.properties = new HashMap<>();
        this.hostName=host.getHostName();
        this.port=host.getPort();
        this.type=type;
        this.destProto = destProto;
    }

    public InetSocketAddress getAddress() throws UnknownHostException {
        return new InetSocketAddress( InetAddress.getByName(hostName),port);
    }
}
