package appExamples2.appExamples.channels.messages;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

@Getter
public class StreamMessage extends ProtoMessage {
    public static final short ID = 102;
    private byte data [];
    private int dataLength;
    private String streamId;
    public StreamMessage(byte [] data,int dataLength,String streamId) {
        super(ID);
        this.data=data;
        this.dataLength = dataLength;
        this.streamId=streamId;
    }

    public static ISerializer<StreamMessage> serializer = new ISerializer<StreamMessage>() {
        @Override
        public void serialize(StreamMessage requestMessage, ByteBuf out) {

        }
        @Override
        public StreamMessage deserialize(ByteBuf in) {
            return new StreamMessage(null,0,null);
        }
    };
}
