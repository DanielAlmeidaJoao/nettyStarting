package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;

@AllArgsConstructor
@Getter
public class ControlDataEntity {
    private InetSocketAddress remotePeer;
    private byte [] data;
}
