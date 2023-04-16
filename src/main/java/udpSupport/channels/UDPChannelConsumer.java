package udpSupport.channels;

import java.net.InetSocketAddress;

public interface UDPChannelConsumer {

    void deliver(byte [] message, InetSocketAddress from);

    void deliverAck(long msgId);
    void messageSentHandler(boolean success, Throwable error, byte [] message, InetSocketAddress dest);
}
