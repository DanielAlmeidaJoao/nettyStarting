package babel.appExamples.protocols;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

import java.io.IOException;
import java.util.Properties;

public class ReceiveFileProtocol extends GenericProtocol {

    public static final short ID = 204;
    public ReceiveFileProtocol(){
        super(ReceiveFileProtocol.class.getSimpleName(),ID);
    }
    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {

    }
}
