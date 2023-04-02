package babel.appExamples.protocols.quicProtocols.echoQuicProtocol.messages;

import babel.appExamples.protocols.quicProtocols.echoQuicProtocol.facctoryMethods.BusinessUtils;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;

@Getter
public class EchoMessage extends ProtoMessage {
    public static final short MSG_ID = 201;
    private Host sender;
    private String message;

    public EchoMessage(Host sender, String message) {
        super(MSG_ID);
        this.sender = sender;
        this.message  = message;
    }
    public static ISerializer<EchoMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(EchoMessage echoMessage, ByteBuf out) throws IOException {
            out.writeBytes(BusinessUtils.gson.toJson(echoMessage).getBytes());
        }
        @Override
        public EchoMessage deserialize(ByteBuf in) throws IOException {
            byte bytes [] = new byte[in.readableBytes()];
            in.readBytes(bytes);
            String data = new String(bytes);
            System.out.println(data);
            EchoMessage echoMessage = BusinessUtils.gson.fromJson(data,EchoMessage.class);
            return echoMessage;
        }
    };
}
