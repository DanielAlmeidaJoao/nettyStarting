package org.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import org.apache.commons.lang3.tuple.Pair;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

public class CustomTCPConnection {
    public final Channel channel;
    public final TransmissionType type;
    public final Pair<InetSocketAddress,String> id;
    public CustomTCPConnection(Channel channel, TransmissionType type,Pair<InetSocketAddress,String> id){
        this.channel=channel;
        this.type=type;
        this.id = id;
    }
}
