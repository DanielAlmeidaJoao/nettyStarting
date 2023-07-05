package quicSupport.utils.streamUtils;

import quicSupport.utils.enums.StreamType;

import java.io.InputStream;
import java.io.OutputStream;

public class BabelOutputStream {
    public final OutputStream outputStream;
    public final StreamType streamType;


    public BabelOutputStream(OutputStream outputStream, StreamType type){
        this.outputStream = outputStream;
        this.streamType = type;
    }


}
