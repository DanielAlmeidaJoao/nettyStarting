package appExamples2.appExamples.channels.babelQuicChannel;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

import java.io.InputStream;

public class BytesMessageSentOrFail extends ProtoMessage {
    public final byte [] data;
    //final short handlerId=id;
    public final int dataLen;
    public final InputStream inputStream;
    public BytesMessageSentOrFail(short id, byte [] data, InputStream inputStream, int dataLen) {
        super(id);
        this.data = data;
        this.dataLen=dataLen;
        this.inputStream = inputStream;
    }
}
