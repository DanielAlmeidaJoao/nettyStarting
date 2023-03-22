package quicSupport.utils.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;

@AllArgsConstructor
@Getter
public class ControlDataEntity {
    private InetSocketAddress remotePeer;
    private byte [] data;
}
