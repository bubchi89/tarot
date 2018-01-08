package tarot.ai;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import tarot.game.Bidding;
import tarot.state.Card;
import tarot.state.Cards;
import tarot.state.Hand;

public class RandomPartnerStrategy extends AbstractPartnerStrategy {
    private static final Random rng = new Random();

    @Override
    public Card call(Hand hand, Bidding bidding) {
        if (!hand.getCards().containsAll(Cards.getRois())) {
            List<Card> asList = ImmutableList.copyOf(Cards.getRois());
            return asList.get(rng.nextInt(asList.size()));
        } else if (!hand.getCards().containsAll(Cards.getDames())) {
            List<Card> asList = ImmutableList.copyOf(Cards.getDames());
            return asList.get(rng.nextInt(asList.size()));
        } else if (!hand.getCards().containsAll(Cards.getCavaliers())) {
            List<Card> asList = ImmutableList.copyOf(Cards.getCavaliers());
            return asList.get(rng.nextInt(asList.size()));
        } else {
            throw new IllegalArgumentException("Supplied hand is not valid for calling a getPartner: " + hand);
        }
    }
}
