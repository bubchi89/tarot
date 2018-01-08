package tarot.ai;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import tarot.game.Bidding;
import tarot.game.Trick.Play;
import tarot.state.Card;
import tarot.state.Hand;

public interface TrickStrategy {
    // TODO: (rliu) This API is iffy because what constitutes a valid handful may change based on the current rules.
    // Perhaps we should pass in that information? Or pull out an interface for the different notions of a handful
    // (basically just official vs. palantir)
    /**
     * Called before and after each play of the first trick for the taker until the taker has played a card.
     *
     * @return A non-empty set of cards constituting a handful or an empty set if the player cannot/should not show
     *         a handful.
     */
    Set<Card> checkHandful(Hand hand, Bidding bidding);

    void handleHandful(Set<Card> shownCards, Bidding bidding, List<String> playerIds, Hand currentHand);

    Card pickCard(List<Play> currentTrick,
                  Hand hand,
                  Bidding bidding,
                  List<String> playerIds,
                  String takerId,
                  @Nullable Card partnerCard);
}
