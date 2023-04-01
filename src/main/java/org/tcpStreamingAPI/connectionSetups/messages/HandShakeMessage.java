package org.tcpStreamingAPI.connectionSetups.messages;

import lombok.Getter;

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

    public HandShakeMessage(String hostName,int port) {
        this.properties = new HashMap<>();
        this.hostName=hostName;
        this.port=port;
    }
    public HandShakeMessage(InetSocketAddress host) {
        this.properties = new HashMap<>();
        this.hostName=host.getHostName();
        this.port=host.getPort();
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
