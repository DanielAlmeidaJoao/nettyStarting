package quicSupport.utils.streamUtils;

import quicSupport.utils.enums.StreamType;

import java.io.InputStream;

public class BabelInputStream {
    public final InputStream inputStream;
    public final StreamType streamType;

    public BabelInputStream(InputStream inputStream, StreamType streamType){
        this.inputStream = inputStream;
        this.streamType = streamType;
    }

}
