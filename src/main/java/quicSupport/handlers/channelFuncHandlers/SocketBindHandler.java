package quicSupport.handlers.channelFuncHandlers;

@FunctionalInterface
public interface SocketBindHandler {
    void execute(boolean success, Throwable error);
}
