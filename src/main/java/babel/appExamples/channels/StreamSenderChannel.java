package babel.appExamples.channels;

import babel.appExamples.channels.messages.StreamMessage;
import babel.appExamples.protocols.ReceiveFileProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Promise;
import org.streamingAPI.client.StreamSender;
import org.streamingAPI.client.StreamSenderImplementation;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;

import static babel.appExamples.channels.StreamReceiverChannel.*;

public class StreamSenderChannel<T> implements IChannel<T> {

    private Host self;
    public final static String NAME = "STREAM_SENDER";

    private final StreamSender streamSender;
    private final ChannelListener<T> listener;

    public StreamSenderChannel(ISerializer<T> serializer, ChannelListener<T> list, Properties properties)throws IOException {
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new Host(addr,port);
        this.listener = list;
        streamSender = new StreamSenderImplementation(addr.getHostName(),port,
                new ChannelFuncHandlers(this::channelActive,this::channelReadConfigData,
                        this::channelRead,this::channelClosed));
        streamSender.connect();
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        boolean triggerSent = false;
        BabelMessage babelMessage = (BabelMessage) msg;
        StreamMessage message = (StreamMessage) babelMessage.getMessage();
        Promise<Void> promise = streamSender.getDefaultEventExecutor().newPromise();
        promise.addListener(future -> {
            if (future.isSuccess() && triggerSent) listener.messageSent(msg, peer);
            else if (!future.isSuccess()) listener.messageFailed(msg, peer, future.cause());
        });

        ByteBuf buf = Unpooled.buffer(message.getDataLength()+8);
        buf.writeInt(message.getDataLength()+4);
        buf.writeShort(babelMessage.getSourceProto());
        buf.writeShort(ReceiveFileProtocol.ID);
        buf.writeBytes(message.getData(),0, message.getDataLength());

        //streamSender.send(buf.array(),buf.readableBytes());
        streamSender.sendWithByteBuf(buf,null);
    }
    @Override
    public void closeConnection(Host peer, int connection) {

    }

    @Override
    public void openConnection(Host peer) {
        streamSender.setHost(self.getAddress().getHostName(),self.getPort());
        streamSender.connect();
    }
    private void channelActive(String channelId){

    }
    private void channelReadConfigData(String channelId, byte [] data){

    }
    private void channelRead(String channelId,byte [] data){

    }
    private void channelClosed(String channelId){

    }
}
