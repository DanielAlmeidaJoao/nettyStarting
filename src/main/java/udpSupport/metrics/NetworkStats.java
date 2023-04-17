package udpSupport.metrics;

import lombok.Getter;

@Getter
public class NetworkStats {
    private long bytesReceived, bytesSent;
    private int messagesReceived, messagesSent;
    private final String name;
    public NetworkStats(String name){
        this.name=name;
    }
    public void addBytesReceived(long bytes){
        messagesReceived++;
        bytesReceived += bytes;
    }
    public void addBytesSent(long bytes){
        messagesSent++;
        bytesSent += bytes;
    }
}
