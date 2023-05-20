package pt.unl.fct.di.novasys.babel.channels;

public abstract class ChannelEvent {

    private final short id;

    public ChannelEvent(short id){
        this.id = id;
    }

    public short getId() {
        return id;
    }
}