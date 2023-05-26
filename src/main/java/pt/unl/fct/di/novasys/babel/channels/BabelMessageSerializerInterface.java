package pt.unl.fct.di.novasys.babel.channels;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public interface BabelMessageSerializerInterface<T> extends ISerializer<T>{

    ISerializer<? extends ProtoMessage> getSerializer(short serializerId);
}
