package quicSupport.utils.entities;

import io.netty.incubator.codec.quic.QuicConnectionStats;
import lombok.AllArgsConstructor;

import java.net.InetSocketAddress;
import java.util.*;

//EVERY STREAM HAS THIS OBJECT????
public class QuicChannelMetrics {
    private final InetSocketAddress self;
    private Map<InetSocketAddress,QuicConnectionMetrics> currentConnections;
    private Map<InetSocketAddress,QuicConnectionMetrics> oldConnections;
    //private Map<InetSocketAddress, QuicConnectionMetrics> metricsMap;

    public QuicChannelMetrics(InetSocketAddress host){
        self=host;
        currentConnections=new HashMap<>();
        oldConnections=new HashMap<>();
    }


    public void addConnectionMetrics(QuicConnectionMetrics connectionMetrics) throws Exception {
        if(currentConnections.put(connectionMetrics.getDest(),connectionMetrics)!=null){
            throw new Exception("TRYING TO REGISTER CONNECTION_METRICS TWICE");
        }
    }
    public void createConnectionMetrics(InetSocketAddress dest, QuicConnectionStats stats, long nBytes ,boolean incoming){
        long sentControlMsgs = 0;
        long receivedControlMsgs = 0;
        long sentControlBytes = 0;
        long receivedControlBytes = 0;
        int createdStream = 0;
        if(incoming){
            receivedControlMsgs = 1;
            receivedControlBytes = nBytes;
        }else {
            sentControlMsgs=1;
            sentControlBytes=nBytes;
            createdStream=1;
        }
        currentConnections.put(dest,new QuicConnectionMetrics(
                dest,0,0,receivedControlBytes,sentControlBytes,
                0,0,
                receivedControlMsgs,sentControlMsgs,1,createdStream,incoming /** ,stats **/
        ));
        System.out.println("ADDED "+dest);
    }

    public QuicConnectionMetrics getConnectionMetrics(InetSocketAddress peer){
        return currentConnections.get(peer);
    }


}
