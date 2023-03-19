package quicSupport.handlers.funcHandlers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.streamingAPI.handlerFunctions.receiver.OpenConnectionFailedHandler;

@AllArgsConstructor
@Getter
public class QuicFuncHandlers {

    private ConnectionActive connectionActive;
    private ConnectionInactive connectionInactive;
    private OpenConnectionFailedHandler connectionError;

    private StreamCreated streamCreated;
    private StreamReader streamReader;
    private StreamClosed streamClosed;
    private StreamErrorHandler errorHandler;
}
