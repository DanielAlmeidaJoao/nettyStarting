package quicSupport.handlers.funcHandlers;

@FunctionalInterface
public interface ConnectionKeepAlive {
    void execute(String connectionId);

}
