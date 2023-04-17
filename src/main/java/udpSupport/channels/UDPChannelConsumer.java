package udpSupport.channels;

import java.net.InetSocketAddress;

public interface UDPChannelConsumer {

    void deliverMessage(byte [] message, InetSocketAddress from);
    void messageSentHandler(boolean success, Throwable error, byte [] message, InetSocketAddress dest);
}
