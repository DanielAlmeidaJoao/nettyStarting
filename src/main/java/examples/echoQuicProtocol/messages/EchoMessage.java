package examples.echoQuicProtocol.messages;

import lombok.Getter;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

import java.net.InetSocketAddress;

@Getter
public class EchoMessage extends ProtoMessage {
    public static final short MSG_ID = 201;
    private Host sender;
    private String message;

    public EchoMessage(){
        super(MSG_ID);
        sender = Host.toBabelHost(new InetSocketAddress(8082));
    }
    public EchoMessage(Host sender, String message) {
        super(MSG_ID);
        this.sender = sender;
        this.message  = message;
    }
    /**
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
            EchoMessage echoMessage = BusinessUtils.gson.fromJson(data,EchoMessage.class);
            return echoMessage;
        }
    }; **/
}
