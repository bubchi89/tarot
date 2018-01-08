package tarot.state;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import tarot.state.Card.Suited;
import tarot.state.Card.Trump;

public class Cards {
    private Cards() {
        // Prevent instantiation
    }

    public static Set<Card> getRois() {
        return ImmutableSet.of(Suited.C_R, Suited.D_R, Suited.H_R, Suited.S_R);
    }

    public static Set<Card> getDames() {
        return ImmutableSet.of(Suited.C_D, Suited.D_D, Suited.H_D, Suited.S_D);
    }

    public static Set<Card> getCavaliers() {
        return ImmutableSet.of(Suited.C_C, Suited.D_C, Suited.H_C, Suited.S_C);
    }

    public static Set<Card> getBouts() {
        return ImmutableSet.of(Trump.ONE, Trump.TWENTY_ONE, Trump.FOOL);
    }

    public static int getDoublePoints(Card c) {
        if (c instanceof Suited) {
            int value = ((Suited) c).getValue();
            if (value <= 10) {
                return 1;
            } else if (value == 11) {
                return 3;
            } else if (value == 12) {
                return 5;
            } else if (value == 13) {
                return 7;
            } else if (value == 14) {
                return 9;
            } else {
                throw new AssertionError("Unknown value for a suited card: " + c);
            }
        } else if (c instanceof Trump) {
            if (getBouts().contains(c)) {
                return 9;
            } else {
                return 1;
            }
        } else {
            throw new AssertionError("Unknown card type: " + c);
        }
    }

    public static Optional<Suit> getSuit(Card card) {
        if (card instanceof Suited) {
            return Optional.of(((Suited) card).getSuit());
        } else if (card instanceof Trump) {
            return Optional.empty();
        } else {
            throw new IllegalArgumentException("Unknown card type: " + card.getClass());
        }
    }

    public static Set<Card> getCardsWithSuit(Set<Card> cards, Suit suit) {
        return Sets.filter(cards, c -> c instanceof Suited && ((Suited) c).getSuit() == suit);
    }

    public static Set<Card> getTrumpLargerThan(Set<Card> cards, @Nullable Trump minTrump) {
        return Sets.filter(
                cards,
                c -> c instanceof Trump && (minTrump == null || Trump.ordering.compare((Trump) c, minTrump) > 0));
    }
}
