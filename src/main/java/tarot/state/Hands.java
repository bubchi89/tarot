package tarot.state;

import java.util.List;

import com.google.common.collect.Ordering;

import tarot.state.Card.Suited;
import tarot.state.Card.Trump;

public class Hands {
    private static final Ordering<Card> defaultSort = Ordering.from((c1, c2) -> {
        if (c1 instanceof Trump && c2 instanceof Suited) {
            return -1;
        } else if (c1 instanceof Suited && c2 instanceof Trump) {
            return 1;
        } else if (c1 instanceof Trump) {
            // Both trump. In hand, we have the greatest "on the left" so do the reverse of the ordering here
            return -Trump.ordering.compare((Trump) c1, (Trump) c2);
        } else {
            // Both non-trump
            return ((Suited) c2).getValue() - ((Suited) c1).getValue();
        }
    });

    private Hands() {
        // Prevent instantiation
    }

    public static List<Card> getSortedCards(Hand hand) {
        return defaultSort.immutableSortedCopy(hand.getCards());
    }
}
