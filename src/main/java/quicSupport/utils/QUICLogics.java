package quicSupport.utils;

import io.netty.buffer.*;
import io.netty.incubator.codec.quic.QuicCodecBuilder;
import io.netty.incubator.codec.quic.QuicCongestionControlAlgorithm;
import org.apache.commons.codec.binary.Hex;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class QUICLogics {
    public static final int WRT_OFFSET=5; //4 BYTES(DATA LEN)+ 1 BYTE (MESSAGE CODE)
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';
    public final static byte KEEP_ALIVE = 'C';
    public final static byte STREAM_CREATED = 'D';

    public final static String idleTimeoutPercentageHB ="idleTimeoutPercentageHB";
    public static final String MAX_IDLE_TIMEOUT_IN_SECONDS = "maxIdleTimeoutInSeconds";
    public static final String INITIAL_MAX_DATA = "initialMaxData";
    public static final String INITIAL_MAX_STREAM_DATA_BIDIRECTIONAL_LOCAL = "initialMaxStreamDataBidirectionalLocal";
    public static final String INITIAL_MAX_STREAM_DATA_BIDIRECTIONAL_REMOTE = "initialMaxStreamDataBidirectionalRemote";
    public static final String INITIAL_MAX_STREAMS_BIDIRECTIONAL = "initialMaxStreamsBidirectional";
    public static final String INITIAL_MAX_STREAMS_UNIDIRECTIONAL = "initialMaxStreamsUnidirectional";

    //public static final String CONNECT_ON_SEND = "AUTO_CONNECT";
    public static final String MAX_ACK_DELAY = "maxAckDelay";





    public static boolean INCOMING_CONNECTION = true;
    public static boolean OUTGOING_CONNECTION = false;

    public static final long maxIdleTimeoutInSeconds=180;
    private static final String initialMaxData="10000000";
                                           //1035368729
    private static final String initialMaxStreamDataBidirectionalLocal="1000000";
    private static final String initialMaxStreamDataBidirectionalRemote="1000000";
    private static final String initialMaxStreamsBidirectional="200";
    private static final String initialMaxStreamsUnidirectional="200";

    private static final String maxAckDelay = "5000";

    public static final String SERVER_KEYSTORE_FILE_KEY = "serverKeystoreFile";
    public static final String SERVER_KEYSTORE_PASSWORD_KEY = "serverKeyStorePassword";
    public static final String SERVER_KEYSTORE_ALIAS_KEY = "serverKeyStoreAlias";

    public static final String CLIENT_KEYSTORE_FILE_KEY = "clientKeystoreFile";
    public static final String CLIENT_KEYSTORE_PASSWORD_KEY = "clientKeyStorePassword";
    public static final String CLIENT_KEYSTORE_ALIAS_KEY = "clientKeyStoreAlias";

    public static final String MAX_UDP_RCV_PAYLOD_SIZE = "maxUdpRCVPayloadSize";
    public static final String MAX_UDP_SND_PAYLOD_SIZE = "maxUdpSNDPayloadSize";

    public static final String CongestionControlAlgorithm = "congestionControlAlgorithm";


    public static ByteBuf bufToWrite(ByteBuf data, byte msgCode, ByteBufAllocator alloc){
        //return new DelimitedMessageWrapper(len,data,msgCode);
        ByteBuf buf = alloc.directBuffer(data.readableBytes()+1);
        buf.writeInt(data.readableBytes());
        buf.writeByte(msgCode);
        buf.writeBytes(data);
        return buf;
    }
    public static ByteBuf bufToWrite(int data, short destProto, byte msgCode, ByteBufAllocator alloc){
        //return new DelimitedMessageWrapper(4,Unpooled.buffer(4).writeInt(data).array(),msgCode);

        ByteBuf buf = alloc.directBuffer(WRT_OFFSET+2);
        buf.writeInt(4+2);
        buf.writeByte(msgCode);
        buf.writeInt(data);
        buf.writeShort(destProto);
        return buf;
    }
    public static boolean sameAddress(InetSocketAddress address, InetSocketAddress socketAddress){
        return address.getHostName().equals(socketAddress.getHostName())&&address.getPort()==socketAddress.getPort();
    }
    public static final String NEW_B_SIZE = "65536"; //1024*64

    public static QuicCongestionControlAlgorithm getCongestionControlAlgorithm(String algo){
        switch (algo){
            case "RENO":return QuicCongestionControlAlgorithm.RENO;
            case "CUBIC":return QuicCongestionControlAlgorithm.CUBIC;
            case "BBR":return QuicCongestionControlAlgorithm.BBR;
            default:System.out.println("UNKNOWN ALGORITHM; USING <CUBIC> AS DEFAULT");
            return QuicCongestionControlAlgorithm.CUBIC;
        }
    }
    public static QuicCodecBuilder addConfigs(QuicCodecBuilder codecBuilder, Properties properties){
        int payloadSizeRecv = Integer.parseInt((String) properties.getOrDefault(MAX_UDP_RCV_PAYLOD_SIZE,"65527"));
        int payloadSizeSend = Integer.parseInt((String) properties.getOrDefault(MAX_UDP_SND_PAYLOD_SIZE,"65527"));

        String congAlgo = (String) properties.getOrDefault(CongestionControlAlgorithm,"CUBIC");
        return codecBuilder
                .maxIdleTimeout(Long.parseLong(properties.getProperty(MAX_IDLE_TIMEOUT_IN_SECONDS,maxIdleTimeoutInSeconds+"")) , TimeUnit.SECONDS)
                .initialMaxData(Long.parseLong(properties.getProperty(INITIAL_MAX_DATA,initialMaxData)))
                .initialMaxStreamDataBidirectionalLocal(Long.parseLong(properties.getProperty(INITIAL_MAX_STREAM_DATA_BIDIRECTIONAL_LOCAL,initialMaxStreamDataBidirectionalLocal)) )
                .initialMaxStreamDataBidirectionalRemote(Long.parseLong(properties.getProperty(INITIAL_MAX_STREAM_DATA_BIDIRECTIONAL_REMOTE,initialMaxStreamDataBidirectionalRemote)))
                .initialMaxStreamsBidirectional(Long.parseLong(properties.getProperty(INITIAL_MAX_STREAMS_BIDIRECTIONAL,initialMaxStreamsBidirectional)))
                .initialMaxStreamsUnidirectional(Long.parseLong(properties.getProperty(INITIAL_MAX_STREAMS_UNIDIRECTIONAL,initialMaxStreamsUnidirectional)))
                .maxAckDelay(Long.parseLong(properties.getProperty(MAX_ACK_DELAY,maxAckDelay)), TimeUnit.MICROSECONDS)
                //.activeMigration(true);
                //.sslTaskExecutor(ImmediateExecutor.INSTANCE)
                .maxRecvUdpPayloadSize(payloadSizeRecv)
                .maxSendUdpPayloadSize(payloadSizeSend)
                .congestionControlAlgorithm(getCongestionControlAlgorithm(congAlgo))
                .hystart(true);
    }

    public static byte[] appendOpToHash(byte[] hash, byte[] op) {
        MessageDigest mDigest;
        try {
            mDigest = MessageDigest.getInstance("sha-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("sha-256 not available...");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(hash);
            baos.write(op);
            return mDigest.digest(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError();
        }
    }
    public static byte[] hash(byte[] hash) {
        MessageDigest mDigest;
        try {
            mDigest = MessageDigest.getInstance("sha-256");
            return mDigest.digest(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("sha-256 not available...");
        }
    }
    private void ttest(byte [] cumulativeHash, byte [] sentData){
        cumulativeHash = appendOpToHash(cumulativeHash,sentData);
        Hex.encodeHexString(cumulativeHash);
    }
    public static TrustManagerFactory trustManagerFactory(String keystoreFilename, String keystorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream(keystoreFilename),keystorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        return tmf;
    }
    public static int compAddresses(InetSocketAddress self,InetSocketAddress other){
        String one = self.getAddress().getHostAddress()+""+self.getPort();
        String two = other.getAddress().getHostAddress()+""+other.getPort();
        int comp = one.compareTo(two);
        //System.out.println(self.getPort()+" COMP "+other.getPort()+" "+comp);
        return comp;
    }
    public static ByteBufAllocator getAllocator(boolean directBuffer) {
        if (directBuffer) {
            return new PooledByteBufAllocator(true);
        } else {
            // Force usage of heap buffers and also ensure memoryAddress() is not not supported.
            return new AbstractByteBufAllocator(false) {

                @Override
                public ByteBuf ioBuffer() {
                    return heapBuffer();
                }

                @Override
                public ByteBuf ioBuffer(int initialCapacity) {
                    return heapBuffer(initialCapacity);
                }

                @Override
                public ByteBuf ioBuffer(int initialCapacity, int maxCapacity) {
                    return heapBuffer(initialCapacity, maxCapacity);
                }

                @Override
                protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity) {
                    return new UnpooledHeapByteBuf(this, initialCapacity, maxCapacity);
                }

                @Override
                protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
                    return new UnpooledDirectByteBuf(this, initialCapacity, maxCapacity);
                }

                @Override
                public boolean isDirectBufferPooled() {
                    return false;
                }
            };
        }
    }
}
