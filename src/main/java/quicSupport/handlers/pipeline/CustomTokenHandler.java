package quicSupport.handlers.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import io.netty.incubator.codec.quic.QuicTokenHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;

import java.net.InetSocketAddress;

public class CustomTokenHandler implements QuicTokenHandler {

    private static final String SERVER_NAME = "netty";
    private static final byte[] SERVER_NAME_BYTES = SERVER_NAME.getBytes(CharsetUtil.US_ASCII);
    private static final ByteBuf SERVER_NAME_BUFFER = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(SERVER_NAME_BYTES)).asReadOnly();

    // Just package-private for unit tests
    static final int MAX_TOKEN_LEN = 10 +
            NetUtil.LOCALHOST6.getAddress().length + SERVER_NAME_BYTES.length;

    public CustomTokenHandler() {

    }

    //public static final InsecureQuicTokenHandler INSTANCE = new InsecureQuicTokenHandler();

    @Override
    public boolean writeToken(ByteBuf out, ByteBuf dcid, InetSocketAddress address) {
        System.out.println("WRITE TOKEN CALLED --- "+address);
        byte[] addr = address.getAddress().getAddress();
        out.writeBytes(SERVER_NAME_BYTES)
                .writeBytes(addr)
                .writeBytes(dcid, dcid.readerIndex(), dcid.readableBytes());
        return true;
    }

    @Override
    public int validateToken(ByteBuf token, InetSocketAddress address) {
        System.out.println("VALIDATE TOKEN CALLED "+address);

        final byte[] addr = address.getAddress().getAddress();

        int minLength = SERVER_NAME_BYTES.length + address.getAddress().getAddress().length;
        if (token.readableBytes() <= SERVER_NAME_BYTES.length + addr.length) {
            return -1;
        }

        if (!SERVER_NAME_BUFFER.equals(token.slice(0, SERVER_NAME_BYTES.length))) {
            return -1;
        }
        ByteBuf addressBuffer = Unpooled.wrappedBuffer(addr);
        try {
            if (!addressBuffer.equals(token.slice(SERVER_NAME_BYTES.length, addr.length))) {
                return -1;
            }
        } finally {
            addressBuffer.release();
        }
        System.out.println("------------- VALID "+minLength);
        return minLength;
    }

    @Override
    public int maxTokenLength() {
        return MAX_TOKEN_LEN;
    }
}
