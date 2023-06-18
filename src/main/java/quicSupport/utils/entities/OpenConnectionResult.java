package quicSupport.utils.entities;

import quicSupport.utils.enums.ConnectionStatus;

import java.net.InetSocketAddress;

public class OpenConnectionResult {
    public static InetSocketAddress peer;
    public static String connectionId;
    public static ConnectionStatus status;
}
