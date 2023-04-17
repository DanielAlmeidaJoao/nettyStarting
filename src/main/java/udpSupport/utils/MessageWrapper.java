package udpSupport.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;

@AllArgsConstructor
@Getter
public class MessageWrapper {
    private byte msgCode;
    private long msgId;
    private byte [] data;
    private InetSocketAddress dest;

}
