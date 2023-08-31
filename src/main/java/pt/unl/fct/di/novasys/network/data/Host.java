package pt.unl.fct.di.novasys.network.data;


import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Represents a node in the network layer, including its IP Address and listen port
 *
 * @author pfouto
 */
public class Host implements Comparable<Host> {
    public final InetSocketAddress address;


    /**
     * Creates a new host with the given address and port
     *
     * @param address The address of the host to create
     */
    public Host(InetSocketAddress address) {
        this.address = address;
    }

    public Host(InetAddress address,int port) {
        if (!(address instanceof Inet4Address))
            throw new AssertionError(address + " not and IPv4 address");
        this.address = new InetSocketAddress(address,port);
    }

    /**
     * Gets the address of this host
     * @return The INetAddress
     */
    public InetAddress getAddress() {
        return address.getAddress();
    }

    /**
     * Gets the port of this host
     * @return  The port
     */
    public int getPort() {
        return address.getPort();
    }

    @Override
    public String toString() {
        return address.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Host)) return false;
        Host o = (Host) other;
        return o.getPort() == address.getPort() && o.address.equals(address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address.getPort(), address.getAddress());
    }

    //Assume always a valid IPv4 address
    @Override
    public int compareTo(Host other) {
        byte [] local = address.getAddress().getAddress();
        byte [] o = other.address.getAddress().getAddress();
        for (int i = 0; i < 4; i++) {
            int cmp = Byte.compare(local[i], o[i]);
            if (cmp != 0) return cmp;
        }
        return Integer.compare(this.address.getPort(), other.address.getPort());
    }


    public static ISerializer<Host> serializer = new ISerializer<Host>() {
        @Override
        public void serialize(Host host, ByteBuf out) {
            out.writeBytes(host.getAddress().getAddress());
            out.writeShort(host.address.getPort());
        }

        @Override
        public Host deserialize(ByteBuf in) throws UnknownHostException {
            byte[] addrBytes = new byte[4];
            in.readBytes(addrBytes);
            int port = in.readShort() & 0xFFFF;
            return new Host(InetAddress.getByAddress(addrBytes),port);
        }
    };


    public static Host toBabelHost(InetSocketAddress socketAddress){
        return new Host(socketAddress);
    }
}
