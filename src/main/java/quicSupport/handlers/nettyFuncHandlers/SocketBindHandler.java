package quicSupport.handlers.nettyFuncHandlers;

@FunctionalInterface
public interface SocketBindHandler {
    void execute(boolean success, Throwable cause);
}
