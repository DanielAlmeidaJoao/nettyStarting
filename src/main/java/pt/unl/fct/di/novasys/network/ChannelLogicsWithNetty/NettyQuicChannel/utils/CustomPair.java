package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CustomPair <L,R> {
    private L left;
    private R right;
}
