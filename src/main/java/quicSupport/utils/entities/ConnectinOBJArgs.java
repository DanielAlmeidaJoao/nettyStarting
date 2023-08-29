package quicSupport.utils.entities;

import lombok.AllArgsConstructor;
import quicSupport.utils.enums.TransmissionType;

@AllArgsConstructor
public class ConnectinOBJArgs {

    public final String conId;
    public final TransmissionType type;
    public final short source,dest;

}
