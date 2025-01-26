package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.utils;

import io.netty.util.concurrent.ScheduledFuture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class Pair {

    private byte [] left;
    @Setter
    private ScheduledFuture right;
}
