package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.utils.funcs;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface OnAckFunction {
    void execute(long msgId, InetSocketAddress sender);
}
