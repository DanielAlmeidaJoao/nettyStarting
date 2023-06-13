package quicSupport.utils.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import quicSupport.utils.enums.TransmissionType;

@AllArgsConstructor
@Getter
public class MessageToByteEncoderParameter {
    public final byte msgCode;
    public final byte [] data;
    public final int dataLen;
    public final TransmissionType transmissionType;
}
