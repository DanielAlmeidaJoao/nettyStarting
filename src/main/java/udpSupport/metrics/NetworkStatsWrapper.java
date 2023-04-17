package udpSupport.metrics;

import lombok.Getter;

@Getter
public class NetworkStatsWrapper {
    private NetworkStats messageStats;
    private NetworkStats ackStats;
    public NetworkStatsWrapper(){
        messageStats = new NetworkStats("messageStats");
        ackStats = new NetworkStats("ackStats");
    }
}
