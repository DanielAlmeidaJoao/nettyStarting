package quicSupport.utils;

import lombok.Getter;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Getter
public class QuicHandShakeMessage {

    public String hostName;
    public int port;
    public String streamId;
    public final TransmissionType transmissionType;
    public final short destProto;
    //private final Map<String,Object> properties;

    public QuicHandShakeMessage(String hostName, int port, String streamId, TransmissionType transmissionType, short destProto) {
        //this.properties = new HashMap<>();
        this.hostName=hostName;
        this.port=port;
        this.streamId=streamId;
        this.transmissionType = transmissionType;
        this.destProto = destProto;
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
