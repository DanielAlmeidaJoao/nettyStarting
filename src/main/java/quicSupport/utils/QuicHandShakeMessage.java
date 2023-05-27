package quicSupport.utils;

import lombok.Getter;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Getter
public class QuicHandShakeMessage {

    public String hostName;
    public int port;
    public String streamId;
    public final ConnectionOrStreamType connectionOrStreamType;
    //private final Map<String,Object> properties;

    public QuicHandShakeMessage(String hostName, int port, String streamId, ConnectionOrStreamType connectionOrStreamType) {
        //this.properties = new HashMap<>();
        this.hostName=hostName;
        this.port=port;
        this.streamId=streamId;
        this.connectionOrStreamType = connectionOrStreamType;
    }


    public InetSocketAddress getAddress() throws UnknownHostException {
        return new InetSocketAddress( InetAddress.getByName(hostName),port);
    }

    /**public void addProperties(String key, Object val){
        properties.put(key,val);
    }
     **/

    /**
    public Object getProperty(String key){
        return properties.get(key);
    }
     **/

}
