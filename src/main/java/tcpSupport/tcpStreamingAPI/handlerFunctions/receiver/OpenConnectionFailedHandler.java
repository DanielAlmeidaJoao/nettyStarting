package tcpSupport.tcpStreamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface OpenConnectionFailedHandler {
    void execute(String channelId,Throwable cause);
}
