package tarot.ai;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import tarot.game.Bidding;
import tarot.game.Trick.Play;
import tarot.game.Tricks;
import tarot.state.Card;
import tarot.state.Card.Trump;
import tarot.state.Cards;
import tarot.state.Hand;
import tarot.state.Suit;

public class RandomTrickStrategy extends AbstractTrickStrategy<RandomTrickStrategy.RoundState> {
    private static final Logger log = LoggerFactory.getLogger(RandomTrickStrategy.class);

    private static final Random rng = new Random();

    @Override
    public Set<Card> checkHandful(Hand hand, Bidding bidding) {
        return ImmutableSet.of();
    }

    @Override
    public void handleHandful(Set<Card> shownCards, Bidding bidding, List<String> playerIds, Hand currentHand) {
        // empty
    }

    @Override
    protected Result<RoundState> pickCardInner(List<Play> currentTrick,
                                               Hand hand,
                                               Bidding bidding, List<String> playerIds, String takerId,
                                               Card partnerCard,
                                               @Nullable RoundState roundState) {
        Set<Card> cards = computeCandidateCards(hand, currentTrick);
        Card card = pickRandomly(cards);
        return ImmutableResult.<RoundState>builder().play(card).roundState(RoundState.EMPTY).build();
    }

    private Set<Card> computeCandidateCards(Hand hand, List<Play> currentTrick) {
        log.debug("Computing candidate cards for current trick:\n\ttrick: {}\n\thand: {}", currentTrick, hand);
        if (currentTrick.isEmpty()) {
            return hand.getCards();
        } else {
            Card firstCard = currentTrick.iterator().next().getCard();
            if (firstCard == Trump.FOOL) {
                return computeCandidateCards(hand, currentTrick.subList(1, currentTrick.size()));
            } else {
                Set<Card> candidateCards = new HashSet<>(computeNonFoolCandidateCards(hand, currentTrick));
                if (hand.getCards().contains(Trump.FOOL)) {
                    candidateCards.add(Trump.FOOL);
                }
                return candidateCards;
            }
        }
    }

    private static Set<Card> computeNonFoolCandidateCards(Hand hand, List<Play> currentTrick) {
        Set<Card> nonFoolCards = hand.getCards().stream().filter(c -> c != Trump.FOOL).collect(Collectors.toSet());
        Optional<Suit> suit = Cards.getSuit(currentTrick.iterator().next().getCard());
        if (suit.isPresent()) {
            Set<Card> cardsWithSuit = Cards.getCardsWithSuit(nonFoolCards, suit.get());
            if (!cardsWithSuit.isEmpty()) {
                return cardsWithSuit;
            }
            // fall through
        }
        Optional<Trump> maxPlayedTrump = Tricks.getMaxTrump(currentTrick);
        Set<Card> largerTrump = Cards.getTrumpLargerThan(nonFoolCards, maxPlayedTrump.orElse(null));
        if (!largerTrump.isEmpty()) {
            return largerTrump;
        } else {
            Set<Card> trump = Sets.filter(nonFoolCards, c -> c instanceof Trump);
            if (!trump.isEmpty()) {
                return trump;
            } else {
                // We can play anything
                return nonFoolCards;
            }
        }
    }

    private static Card pickRandomly(Set<Card> cards) {
        return Iterators.get(cards.iterator(), rng.nextInt(cards.size()));
    }

    enum RoundState {
        EMPTY
    }
}
