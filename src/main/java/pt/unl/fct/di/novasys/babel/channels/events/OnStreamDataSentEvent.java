package pt.unl.fct.di.novasys.babel.channels.events;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.InputStream;

public class OnStreamDataSentEvent extends ProtoMessage {
    public static final short ID = 503;
    public final byte [] sentBytes;
    public final InputStream inputStream;
    public final long dataLen;
    public final Throwable error;
    public final String connectionID;
    public final Host host;
    public OnStreamDataSentEvent(byte []data, InputStream inputStream, long dataLen, Throwable error, String conID, Host host) {
        super(ID);
        this.sentBytes = data;
        this.inputStream = inputStream;
        this.dataLen = dataLen;
        this.connectionID = conID;
        this.host = host;
        this.error = error;
    }
}
