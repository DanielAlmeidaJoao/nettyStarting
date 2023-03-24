package quicSupport.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CustomPair <L,R> {
    private L left;
    private R right;
}
