package quicSupport.utils.streamUtils;

public class BabelInBytesWrapper {


    public final int availableBytes;

    public final byte [] bytes;

    public BabelInBytesWrapper(byte [] bytes1){
        this.availableBytes = bytes1.length;
        this.bytes = bytes1;
    }
}
