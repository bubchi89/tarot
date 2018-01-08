package tarot.ai;

import java.util.Set;

import javax.annotation.Nullable;

import tarot.game.Bidding;
import tarot.state.Bids;
import tarot.state.Card;
import tarot.state.Hand;

public abstract class AbstractDogStrategy implements DogStrategy {
    @Override
    public Set<Card> chooseAside(Hand hand, Set<Card> dog, Bidding bidding, @Nullable Card partnerCard) {
        if (!Bids.canSeeDog(bidding.getBid().get())) {
            throw new IllegalArgumentException("Should not be able to see dog with bid: " + bidding);
        } else {
            return chooseAsideInner(hand, dog, bidding, partnerCard);
        }
    }

    protected abstract Set<Card> chooseAsideInner(Hand hand, Set<Card> dog, Bidding bidding, @Nullable Card partnerCard);
}
