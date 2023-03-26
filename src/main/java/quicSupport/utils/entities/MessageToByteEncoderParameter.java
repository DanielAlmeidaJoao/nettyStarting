package quicSupport.utils.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageToByteEncoderParameter {
    private final byte msgCode;
    private final byte [] data;
    private final int dataLen;
}
