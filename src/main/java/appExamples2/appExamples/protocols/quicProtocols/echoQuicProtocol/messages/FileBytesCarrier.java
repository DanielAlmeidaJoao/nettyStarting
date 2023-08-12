package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;

public class FileBytesCarrier extends ProtoMessage {

    public final byte [] data;
    public final int len;

    public static final short ID = 109;
    public FileBytesCarrier(byte [] data,int len){
        super(ID);
        this.data=data;
        this.len=len;
    }

    public static ISerializer<FileBytesCarrier> serializer = new ISerializer<>() {
        @Override
        public void serialize(FileBytesCarrier fileBytesCarrier, ByteBuf out) throws IOException {
            out.writeBytes(fileBytesCarrier.data,0,fileBytesCarrier.len);
        }
        @Override
        public FileBytesCarrier deserialize(ByteBuf in) throws IOException {
            byte bytes [] = new byte[in.readableBytes()];
            in.readBytes(bytes);
            return new FileBytesCarrier(bytes,bytes.length);
        }
    };
}
