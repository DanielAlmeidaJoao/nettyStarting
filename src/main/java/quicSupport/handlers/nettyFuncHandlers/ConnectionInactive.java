package quicSupport.handlers.nettyFuncHandlers;

@FunctionalInterface
public interface ConnectionInactive {
    void execute(String channelId);
}
