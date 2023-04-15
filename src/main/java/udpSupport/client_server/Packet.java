package udpSupport.client_server;

import java.net.InetSocketAddress;

public class Packet {
    private String data;
    private InetSocketAddress recipient;

    public Packet(String data, InetSocketAddress recipient) {
        this.data = data;
        this.recipient = recipient;
    }

    public String getData() {
        return data;
    }

    public InetSocketAddress getRecipient() {
        return recipient;
    }
}
