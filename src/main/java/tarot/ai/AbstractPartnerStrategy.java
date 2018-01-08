package tarot.ai;

import java.util.Optional;

import com.google.common.collect.Sets;

import tarot.game.Bidding;
import tarot.state.Card;
import tarot.state.Cards;
import tarot.state.Hand;

public abstract class AbstractPartnerStrategy implements PartnerStrategy {
    @Override
    public Optional<Card> callIfPossible(Hand hand, Bidding bidding) {
        if (canCallPartner(hand)) {
            return Optional.of(call(hand, bidding));
        } else {
            return Optional.empty();
        }
    }

    private static boolean canCallPartner(Hand hand) {
        return !hand.getCards().containsAll(
                Sets.union(Sets.union(Cards.getRois(), Cards.getDames()), Cards.getCavaliers()));
    }

    protected abstract Card call(Hand hand, Bidding bidding);
}
