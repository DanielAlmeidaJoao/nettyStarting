package quicSupport.utils.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageDecoderOutput {
    private final byte msgCode;
    private final byte [] data;
}
