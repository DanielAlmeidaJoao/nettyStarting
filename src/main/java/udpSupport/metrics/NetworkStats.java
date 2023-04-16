package udpSupport.metrics;

import lombok.Getter;

@Getter
public class NetworkStats {
    private long bytesReceived, bytesSent;
    private int messagesReceived, messagesSent;
    public void addBytesReceived(long bytes){
        messagesReceived++;
        bytesReceived += bytes;
    }
    public void addBytesSent(long bytes){
        messagesSent++;
        bytesSent += bytes;
    }
}
