package babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.Metric;

public class Instant extends Metric {

    Object lastLog = null;

    public Instant(String name) {
        super(name, false, -1, true, false);
    }

    public synchronized void log(Object obj) {
        lastLog = obj;
        onChange();
    }

    @Override
    protected synchronized void reset() {
    }

    @Override
    protected synchronized String computeValue() {
        return lastLog.toString();
    }
}
