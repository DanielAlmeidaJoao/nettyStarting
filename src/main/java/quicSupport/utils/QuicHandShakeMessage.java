package quicSupport.utils;

import lombok.Getter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@Getter
public class QuicHandShakeMessage {

    private String hostName;
    private int port;
    private String streamId;
    private final Map<String,Object> properties;

    public QuicHandShakeMessage(String hostName, int port,String streamId) {
        this.properties = new HashMap<>();
        this.hostName=hostName;
        this.port=port;
        this.streamId=streamId;
    }
    public QuicHandShakeMessage(InetSocketAddress host) {
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
