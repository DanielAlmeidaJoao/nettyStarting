package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages;

import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.facctoryMethods.BusinessUtils;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

import java.io.IOException;

@Getter
public class EchoMessage2 extends ProtoMessage {
    public static final short MSG_ID = 204;
    private Host sender;
    private String message;

    public EchoMessage2(Host sender, String message) {
        super(MSG_ID);
        this.sender = sender;
        this.message  = message;
    }
    public static ISerializer<EchoMessage2> serializer = new ISerializer<>() {
        @Override
        public void serialize(EchoMessage2 echoMessage, ByteBuf out) throws IOException {
            out.writeBytes(BusinessUtils.gson.toJson(echoMessage).getBytes());
        }
        @Override
        public EchoMessage2 deserialize(ByteBuf in) throws IOException {
            byte bytes [] = new byte[in.readableBytes()];
            in.readBytes(bytes);
            String data = new String(bytes);
            EchoMessage2 echoMessage = BusinessUtils.gson.fromJson(data, EchoMessage2.class);
            return echoMessage;
        }
    };
}
