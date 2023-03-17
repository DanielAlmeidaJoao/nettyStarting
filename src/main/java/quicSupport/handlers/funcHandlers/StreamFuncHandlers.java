package quicSupport.handlers.funcHandlers;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class StreamFuncHandlers {

    private StreamCreated streamCreated;
    private StreamClosed streamClosed;
    private StreamErrorHandler errorHandler;
}
