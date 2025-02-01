package pt.unl.fct.di.novasys.babel.generic;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

/**
 * Abstract Message class to be extended by protocol-specific messages.
 */
public abstract class ProtoMessage {

    private final short id;

    public ProtoMessage(short id){
        this.id = id;
    }

    public short getId() {
        return id;
    }

    private void writeString(ByteBuf out, String value){
        if(value == null){
            out.writeInt(0);
        } else {
            out.writeInt(value.length());
            out.writeCharSequence(value,Charset.defaultCharset());
        }
    }
    private String readString(ByteBuf in, int size){
        return in.readCharSequence(size,Charset.defaultCharset()).toString();
    }

    private void serializeMessage(ByteBuf out) {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {

                if(Modifier.isFinal(field.getModifiers())||Modifier.isStatic(field.getModifiers())){
                    continue;
                }
                field.setAccessible(true);
                switch (field.getType().getSimpleName()) {
                    case "byte":
                        out.writeByte(field.getByte(this));
                        break;
                    case "boolean":
                        out.writeBoolean(field.getBoolean(this));
                        break;
                    case "short":
                        out.writeShort(field.getByte(this));
                        break;
                    case "int":
                        out.writeInt(field.getInt(this));
                        break;
                    case "float":
                        out.writeFloat(field.getFloat(this));
                        break;
                    case "long":
                        out.writeLong(field.getLong(this));
                        break;
                    case "char":
                        out.writeChar(field.getChar(this));
                        break;
                    case "String":
                        writeString(out,(String) field.get(this));
                        break;
                    case "double":
                        out.writeDouble(field.getDouble(this));
                        break;
                    case "byte[]":
                        byte [] bytes = (byte[]) field.get(this);
                        if (bytes == null){
                            out.writeInt(0);
                        } else {
                            out.writeInt(bytes.length);
                            out.writeBytes(bytes);
                        }
                        break;
                    default:
                        Object object = field.get(this);

                        if (object == null){
                            out.writeBoolean(false);
                        } else {
                            out.writeBoolean(true);
                            if (ProtoMessage.class.isAssignableFrom(field.getType())) {
                                ProtoMessage aux = (ProtoMessage) object;
                                aux.serializeMessage(out);
                            }else if (Host.class.isAssignableFrom(field.getType())) {
                                Host.serializer.serialize((Host) object,out);
                            } else {
                                throw new RuntimeException("Error trying to serialize field: " + field.getName());
                            }
                        }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deserializeMessage(ByteBuf in) {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                if(Modifier.isFinal(field.getModifiers())||Modifier.isStatic(field.getModifiers())){
                    continue;
                }
                field.setAccessible(true);
                switch (field.getType().getSimpleName()) {
                    case "byte":
                        field.setByte(this,in.readByte());
                        break;
                    case "boolean":
                        field.setBoolean(this,in.readBoolean());
                        break;
                    case "short":
                        field.setShort(this,in.readShort());
                        break;
                    case "int":
                        field.setInt(this,in.readInt());
                        break;
                    case "float":
                        field.setFloat(this,in.readFloat());
                        break;
                    case "long":
                        field.setLong(this,in.readLong());
                        break;
                    case "char":
                        field.setChar(this,in.readChar());
                        break;
                    case "String":
                        int read = in.readInt();
                        if(read > 0){
                            field.set(this,readString(in,read));
                        }
                        break;
                    case "double":
                        field.setDouble(this,in.readDouble());
                        break;
                    case "byte[]":
                        byte [] array = new byte[in.readInt()];
                        in.readBytes(array);
                        field.set(this,array);
                        break;
                    default:
                        boolean isValidObject = in.readBoolean();
                        if (isValidObject){
                            if (ProtoMessage.class.isAssignableFrom(field.getType())) {
                                Class cc = field.getType();
                                ProtoMessage protoMessage = getNewEmptyInstance(cc);
                                protoMessage.deserializeMessage(in);
                                field.set(this,protoMessage);
                            }else if (Host.class.isAssignableFrom(field.getType())) {
                                field.set(this,Host.serializer.deserialize(in));
                            }else {
                                throw new RuntimeException(this.getClass().getName()+": Error trying to deserialize field: " + field.getName());
                            }
                        }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <V extends ProtoMessage> ProtoMessage getNewEmptyInstance(Class<V> vClass){
        try {
            for (Constructor<?> constructor : vClass.getConstructors()) {
                if(constructor.getParameterCount() == 0){
                    return (V) constructor.newInstance();
                }
            }
        } catch (Exception e){
            throw new RuntimeException("Every class that extends ProtoMessage needs an empty constructor!");
        }
        return null;
    };

    public <V extends ProtoMessage> ISerializer<V> newSerializer(final Class<?> zclass){
        V emptyMessage = null;
        try{
            for (Constructor<?> constructor : zclass.getConstructors()) {
                if(constructor.getParameterCount() == 0){
                    emptyMessage = (V) constructor.newInstance();
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if( emptyMessage == null ){
            throw new RuntimeException(zclass.getName() + " IS MISSING AN EMPTY CONSTRUCTOR!");
        }
        final V finalEmptyMessage = emptyMessage;

        return new ISerializer<>() {
            @Override
            public void serialize(ProtoMessage protoMessage, ByteBuf out) throws IOException {
                protoMessage.serializeMessage(out);
            }

            @Override
            public V deserialize(ByteBuf in) throws IOException {
                ProtoMessage protoMessage = finalEmptyMessage.getNewEmptyInstance(finalEmptyMessage.getClass());
                protoMessage.deserializeMessage(in);

                return (V) protoMessage;
            }
        };
    };

}
