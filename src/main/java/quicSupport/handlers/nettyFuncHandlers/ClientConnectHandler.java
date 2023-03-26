package quicSupport.handlers.nettyFuncHandlers;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface ClientConnectHandler {
    void execute(boolean success, Throwable cause,InetSocketAddress remote);
}
