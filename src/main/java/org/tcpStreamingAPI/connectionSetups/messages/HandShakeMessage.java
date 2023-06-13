package org.tcpStreamingAPI.connectionSetups.messages;

import lombok.Getter;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@Getter
public class HandShakeMessage {

    private String hostName;
    private int port;
    private final Map<String,Object> properties;
    public final TransmissionType type;

    public HandShakeMessage(String hostName, int port, TransmissionType type) {
        this.properties = new HashMap<>();
        this.hostName=hostName;
        this.port=port;
        this.type=type;
    }
    public HandShakeMessage(InetSocketAddress host, TransmissionType type) {
        this.properties = new HashMap<>();
        this.hostName=host.getHostName();
        this.port=host.getPort();
        this.type=type;
    }

    public InetSocketAddress getAddress() throws UnknownHostException {
        return new InetSocketAddress( InetAddress.getByName(hostName),port);
    }

    public void addProperties(String key, Object val){
        properties.put(key,val);
    }

    public Object getProperty(String key){
        return properties.get(key);
    }

}
