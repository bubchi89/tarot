package tarot.ai;

import java.util.Set;

import javax.annotation.Nullable;

import tarot.game.Bidding;
import tarot.state.Bid;
import tarot.state.Bids;
import tarot.state.Card;
import tarot.state.Hand;

public interface DogStrategy {
    /**
     * @throws IllegalArgumentException if {@link Bids#canSeeDog(Bid)} is false
     */
    Set<Card> chooseAside(Hand hand, Set<Card> dog, Bidding bidding, @Nullable Card partnerCard);

}
