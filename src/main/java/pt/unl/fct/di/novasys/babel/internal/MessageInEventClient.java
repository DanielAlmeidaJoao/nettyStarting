package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * An abstract class that represents a protocol message
 *
 * @see InternalEvent
 * @see GenericProtocol
 */
public class MessageInEventClient<M extends ProtoMessage> extends MessageInEvent {

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public MessageInEventClient(BabelMessage msg, Host from, int channelId, String connectionId) {
        super(msg, from, channelId, connectionId);
    }


    public M getNetwrokMessage(){
        return (M) super.getMsg().getMessage();
    }

}
