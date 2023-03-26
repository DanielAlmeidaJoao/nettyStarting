package quicSupport.handlers.nettyFuncHandlers;

@FunctionalInterface
public interface ConnectionKeepAlive {
    void execute(String connectionId);

}
