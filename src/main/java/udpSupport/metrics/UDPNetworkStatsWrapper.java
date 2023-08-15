package udpSupport.metrics;

import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
public class UDPNetworkStatsWrapper {
    private InetSocketAddress dest;
    public final NetworkStats totalMessageStats;
    public final NetworkStats sentAckedMessageStats;
    public final NetworkStats ackStats;

    @Override
    public UDPNetworkStatsWrapper clone(){
        return new UDPNetworkStatsWrapper(dest,totalMessageStats.clone(),sentAckedMessageStats.clone(),ackStats.clone());
    }

    public UDPNetworkStatsWrapper(InetSocketAddress host){
        totalMessageStats = new NetworkStats("totalMessageStats");
        sentAckedMessageStats = new NetworkStats("sentAckedMessageStats");
        ackStats = new NetworkStats("ackStats");
        dest=host;
    }

    public UDPNetworkStatsWrapper(InetSocketAddress host, UDPNetworkStatsWrapper UDPNetworkStatsWrapper){
        dest=host;
        totalMessageStats = UDPNetworkStatsWrapper.totalMessageStats;
        sentAckedMessageStats = UDPNetworkStatsWrapper.sentAckedMessageStats;
        ackStats = UDPNetworkStatsWrapper.ackStats;
    }
    private UDPNetworkStatsWrapper(InetSocketAddress host, NetworkStats totalMessageStats, NetworkStats sentAckedMessageStats, NetworkStats ackStats){
        this.dest=host;
        this.totalMessageStats = totalMessageStats;
        this.sentAckedMessageStats = sentAckedMessageStats;
        this.ackStats = ackStats;
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
