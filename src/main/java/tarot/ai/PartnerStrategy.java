package tarot.ai;

import java.util.Optional;

import tarot.game.Bidding;
import tarot.state.Card;
import tarot.state.Hand;

public interface PartnerStrategy {
    Optional<Card> callIfPossible(Hand hand, Bidding bidding);
}
