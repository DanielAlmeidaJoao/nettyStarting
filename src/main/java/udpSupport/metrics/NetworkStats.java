package udpSupport.metrics;

import lombok.Getter;

@Getter
public class NetworkStats {
    private long bytesReceived, bytesSent;
    private int messagesReceived, messagesSent;

    private long sumRTT;
    private final String name;
    public NetworkStats(String name){
        this.name=name;
        sumRTT = 0;
    }
    public void addBytesReceived(long bytes){
        messagesReceived++;
        bytesReceived += bytes;
    }
    public void addBytesSent(long bytes){
        messagesSent++;
        bytesSent += bytes;
    }
    public void addRTT(long timeMillis){
        //averageRTT = sumRTT/bytesSent
        sumRTT +=timeMillis;
    }
}
