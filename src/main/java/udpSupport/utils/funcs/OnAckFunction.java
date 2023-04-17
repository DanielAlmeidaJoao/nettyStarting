package udpSupport.utils.funcs;

@FunctionalInterface
public interface OnAckFunction {
    void execute(long msgId);
}
