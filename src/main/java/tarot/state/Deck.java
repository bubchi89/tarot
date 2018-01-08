package tarot.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.immutables.value.Value;

import tarot.state.Card.Suited;
import tarot.state.Card.Trump;

public class Deck {
    private final LinkedHashSet<Card> cards;

    private Deck(LinkedHashSet<Card> cards) {
        this.cards = cards;
    }

    public static Deck unshuffled() {
        LinkedHashSet<Card> cards = new LinkedHashSet<>();
        cards.addAll(EnumSet.allOf(Suited.class));
        cards.addAll(EnumSet.allOf(Trump.class));
        return new Deck(cards);
    }

    public LinkedHashSet<Card> getCards() {
        return cards;
    }

    public void softShuffle() {
        // TODO: make this an actual soft-shuffle
        List<Card> asList = new ArrayList<>(this.cards);
        Collections.shuffle(asList);
        cards.clear();
        cards.addAll(asList);
    }

    @Value.Immutable
    public interface Deal {
        List<Hand> getHands();

        Set<Card> getDog();
    }
}
