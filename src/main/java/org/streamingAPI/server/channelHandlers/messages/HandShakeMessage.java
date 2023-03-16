package org.streamingAPI.server.channelHandlers.messages;

import lombok.Getter;

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

    public void addProperties(String key, Object val){
        properties.put(key,val);
    }

    public Object getProperty(String key){
        return properties.get(key);
    }

}
