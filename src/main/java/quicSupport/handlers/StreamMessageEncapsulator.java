package quicSupport.handlers;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class StreamMessageEncapsulator {

    private byte msgType;
    private byte [] data;
}
