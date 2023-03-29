package babel.appExamples.channels;

import babel.appExamples.channels.messages.EndOfStreaming;
import babel.appExamples.channels.messages.StreamMessage;
import babel.appExamples.protocols.ReceiveFileProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.channel.SingleThreadedStreamingChannel;
import org.streamingAPI.connectionSetups.messages.HandShakeMessage;
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

public class BabelStreamingChannel<T> extends SingleThreadedStreamingChannel implements IChannel<T> {

    private static final Logger logger = LogManager.getLogger(BabelStreamingChannel.class);

    private final ChannelListener<T> listener;

    public BabelStreamingChannel(ISerializer<T> serializer, ChannelListener<T> list, Properties properties) throws IOException {
        super(properties);
        this.listener = list;
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        BabelMessage babelMessage = (BabelMessage) msg;
        StreamMessage message = (StreamMessage) babelMessage.getMessage();

        ByteBuf buf = Unpooled.buffer(message.getDataLength()+8);
        buf.writeInt(message.getDataLength()+4);
        buf.writeShort(babelMessage.getSourceProto());
        buf.writeShort(ReceiveFileProtocol.ID);
        buf.writeBytes(message.getData(),0, message.getDataLength());
        send(buf,toInetSocketAddress(peer));
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
    public void onChannelInactive(InetSocketAddress peer) {
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
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer) {
        listener.deliverEvent(new InConnectionUp(toBabelHost(peer)));
    }

    @Override
    public void sendFailed(InetSocketAddress peer, Throwable reason) {

    }

    @Override
    public void sendSuccess(byte[] data, InetSocketAddress peer) {

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
