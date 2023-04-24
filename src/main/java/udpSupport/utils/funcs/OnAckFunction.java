package udpSupport.utils.funcs;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface OnAckFunction {
    void execute(long msgId, InetSocketAddress sender);
}
