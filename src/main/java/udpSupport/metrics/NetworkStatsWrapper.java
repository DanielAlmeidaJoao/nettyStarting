package udpSupport.metrics;

import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
public class NetworkStatsWrapper {
    private InetSocketAddress dest;
    public final NetworkStats totalMessageStats;
    public final NetworkStats sentAckedMessageStats;
    public final NetworkStats ackStats;

    public NetworkStatsWrapper(InetSocketAddress host){
        totalMessageStats = new NetworkStats("totalMessageStats");
        sentAckedMessageStats = new NetworkStats("sentAckedMessageStats");
        ackStats = new NetworkStats("ackStats");
        dest=host;
    }

    public NetworkStatsWrapper(InetSocketAddress host, NetworkStatsWrapper networkStatsWrapper){
        dest=host;
        totalMessageStats = networkStatsWrapper.totalMessageStats;
        sentAckedMessageStats = networkStatsWrapper.sentAckedMessageStats;
        ackStats = networkStatsWrapper.ackStats;
    }

    public NetworkStats getStats(NetworkStatsKindEnum key) {
        switch (key){
            case MESSAGE_STATS: return totalMessageStats;
            case EFFECTIVE_SENT_DELIVERED: return sentAckedMessageStats;
            case ACK_STATS: return ackStats;
            default: throw new RuntimeException("UNKNOWN NetworkStatsKindEnum: "+key);
        }
    }
    public NetworkStats [] statsCollection(){
        NetworkStats stats [] = {totalMessageStats, sentAckedMessageStats,ackStats};
        return stats;
    }


}
