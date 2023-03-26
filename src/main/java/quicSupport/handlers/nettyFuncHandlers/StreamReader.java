package quicSupport.handlers.nettyFuncHandlers;

@FunctionalInterface
public interface StreamReader {
    void execute(String streamId, byte [] data);

}
