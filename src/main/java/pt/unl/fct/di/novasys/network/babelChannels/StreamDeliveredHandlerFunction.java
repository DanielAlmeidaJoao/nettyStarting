package pt.unl.fct.di.novasys.network.babelChannels;

import pt.unl.fct.di.novasys.babel.internal.BabelStreamDeliveryEvent;

@FunctionalInterface
public interface StreamDeliveredHandlerFunction {
    void execute(BabelStreamDeliveryEvent event);
}
