package appExamples2.appExamples.channels.babelQuicChannel;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class BytesMessageSentOrFail extends ProtoMessage {
    final byte [] bytes;
    //final short handlerId=id;
    final int dataLen;
    public BytesMessageSentOrFail(short id, byte [] data, int dataLen) {
        super(id);
        this.bytes=data;
        this.dataLen=dataLen;
    }
}
