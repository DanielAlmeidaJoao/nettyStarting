package udpSupport.metrics;

import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
public class NetworkStatsWrapper {
    private InetSocketAddress dest;
    private NetworkStats messageStats;
    private NetworkStats ackStats;
    public NetworkStatsWrapper(InetSocketAddress host){
        messageStats = new NetworkStats("messageStats");
        ackStats = new NetworkStats("ackStats");
        dest=host;
    }
    public NetworkStatsWrapper(InetSocketAddress host, NetworkStats messageStats, NetworkStats ackStats){
        this.messageStats = messageStats;
        this.ackStats =ackStats;
        dest=host;
    }
}
