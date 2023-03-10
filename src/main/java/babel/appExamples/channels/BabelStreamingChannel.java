package babel.appExamples.channels;

import babel.appExamples.channels.messages.EndOfStreaming;
import babel.appExamples.channels.messages.StreamMessage;
import babel.appExamples.protocols.ReceiveFileProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.channel.StreamingChannel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class BabelStreamingChannel<T> extends StreamingChannel implements IChannel<T> {

    private static final Logger logger = LogManager.getLogger(BabelStreamingChannel.class);

    private final ChannelListener<T> listener;

    public BabelStreamingChannel(ISerializer<T> serializer, ChannelListener<T> list, Properties properties) throws IOException {
        super(properties);
        this.listener = list;
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        boolean triggerSent = false;
        BabelMessage babelMessage = (BabelMessage) msg;
        StreamMessage message = (StreamMessage) babelMessage.getMessage();
        Promise<Void> promise = getExecutor().newPromise();
        promise.addListener(future -> {
            if (future.isSuccess() && triggerSent) listener.messageSent(msg, peer);
            else if (!future.isSuccess()) listener.messageFailed(msg, peer, future.cause());
        });

        ByteBuf buf = Unpooled.buffer(message.getDataLength()+8);
        buf.writeInt(message.getDataLength()+4);
        buf.writeShort(babelMessage.getSourceProto());
        buf.writeShort(ReceiveFileProtocol.ID);
        buf.writeBytes(message.getData(),0, message.getDataLength());
        sendDelimited(buf,null,toInetSocketAddress(peer));
    }

    @Override
    public void closeConnection(Host peer, int connection) {
        super.closeConnection(toInetSocketAddress(peer));
    }

    @Override
    public void openConnection(Host peer) {
        super.openConnection(toInetSocketAddress(peer));
    }

    @Override
    public void onChannelClosed(InetSocketAddress peer) {
        Throwable cause = new Throwable(String.format("CHANNEL %S CLOSED.",peer));
        listener.deliverEvent(new InConnectionDown(toBabelHost(peer), cause));
    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes,InetSocketAddress from) {
        ProtoMessage p;
        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        int dataLen=buf.readableBytes()-4;
        short src = buf.readShort();
        short dest=buf.readShort();
        if(bytes.length==4){
            p = new EndOfStreaming();
        }else {
            byte [] appData = new byte[dataLen];
            buf.readBytes(appData,0,dataLen);
            p = new StreamMessage(appData,dataLen,channelId);
        }

        BabelMessage babelMessage = new BabelMessage(p,src,dest);
        listener.deliverMessage((T) babelMessage,toBabelHost(from));
    }
    @Override
    public void channelReadConfigData(String s, byte[] bytes) {

    }

    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer) {
        listener.deliverEvent(new InConnectionUp(toBabelHost(peer)));
    }

    @Override
    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {
        logger.info("CONNECTION TO {} FAILED. CAUSE = {}.",peer,cause);
    }

    private InetSocketAddress toInetSocketAddress(Host host){
        return new InetSocketAddress(host.getAddress().getHostAddress(),host.getPort());
    }
    private Host toBabelHost(InetSocketAddress address){
        return new Host(address.getAddress(),address.getPort());
    }
}
