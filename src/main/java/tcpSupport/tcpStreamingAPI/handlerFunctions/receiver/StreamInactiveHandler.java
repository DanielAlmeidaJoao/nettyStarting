package tcpSupport.tcpStreamingAPI.handlerFunctions.receiver;

//END OF STREAMING FUNCTION
@FunctionalInterface
public interface StreamInactiveHandler {
    public void execute(String id);
}
