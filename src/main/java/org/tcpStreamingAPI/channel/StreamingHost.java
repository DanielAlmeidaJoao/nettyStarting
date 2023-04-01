package org.tcpStreamingAPI.channel;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Objects;

public class StreamingHost implements Comparable<StreamingHost> {
        private final int port;
        private final InetAddress address;
        private final byte[] addressBytes;

        /**
         * Creates a new host with the given address and port
         *
         * @param address The address of the host to create
         * @param port    The port of the host to create
         */
        public StreamingHost(InetAddress address, int port) {
            this(address, address.getAddress(), port);
        }

        private StreamingHost(InetAddress address, byte[] addressBytes, int port) {
            if (!(address instanceof Inet4Address))
                throw new AssertionError(address + " not and IPv4 address");
            this.address = address;
            this.port = port;
            this.addressBytes = addressBytes;
            assert addressBytes.length == 4;
        }

        /**
         * Gets the address of this host
         * @return The INetAddress
         */
        public InetAddress getAddress() {
            return address;
        }

        /**
         * Gets the port of this host
         * @return  The port
         */
        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return address.getHostAddress() + ":" + port;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof StreamingHost)) return false;
            StreamingHost o = (StreamingHost) other;
            return o.port == port && o.address.equals(address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(port, address);
        }

        //Assume always a valid IPv4 address
        @Override
        public int compareTo(StreamingHost other) {
            for (int i = 0; i < 4; i++) {
                int cmp = Byte.compare(this.addressBytes[i], other.addressBytes[i]);
                if (cmp != 0) return cmp;
            }
            return Integer.compare(this.port, other.port);
        }
    }
