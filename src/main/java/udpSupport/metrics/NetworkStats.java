package udpSupport.metrics;

import lombok.Getter;

@Getter
public class NetworkStats {
    private long bytesReceived, bytesSent;
    private int messagesReceived, messagesSent, messagesAcked;
    private long sumRTT;
    private final String name;
    @Override
    public NetworkStats clone(){
        NetworkStats networkStats = new NetworkStats(name);
        networkStats.bytesReceived = bytesReceived;
        networkStats.bytesSent = bytesSent;
        networkStats.messagesReceived=messagesReceived;
        networkStats.messagesSent=messagesSent;
        networkStats.messagesAcked=messagesAcked;
        networkStats.sumRTT=sumRTT;
        return networkStats;
    }


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
        messagesAcked++;
        sumRTT +=timeMillis;
    }
}
