package tarot.state;

import java.util.Set;

import org.immutables.value.Value;

@Value.Immutable
public interface Hand {
    @Value.Parameter
    Set<Card> getCards();
}
