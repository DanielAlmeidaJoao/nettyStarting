package udpSupport.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class Pair <L,R>{

    private L left;
    @Setter
    private R right;
}
