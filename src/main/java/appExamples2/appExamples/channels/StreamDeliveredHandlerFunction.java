package appExamples2.appExamples.channels;

import pt.unl.fct.di.novasys.babel.internal.BabelStreamDeliveryEvent;

@FunctionalInterface
public interface StreamDeliveredHandlerFunction {
    void execute(BabelStreamDeliveryEvent event);
}
