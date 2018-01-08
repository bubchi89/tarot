package tarot.game;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;

import tarot.game.Trick.Play;
import tarot.state.Card;
import tarot.state.Card.Suited;
import tarot.state.Card.Trump;
import tarot.state.Cards;
import tarot.state.Hand;
import tarot.state.Suit;

public class Tricks {
    private static final Logger log = LoggerFactory.getLogger(Tricks.class);

    private Tricks() {
        // Prevent instantiation
    }

    public static void verifyPlay(Hand hand, Card card, List<Play> currentTrick) {
        log.debug("Verifying play:\n\tcard: {}\n\ttrick: {}\n\thand: {}", card, currentTrick, hand.getCards());

        Verify.verify(hand.getCards().contains(card), "Can only play cards in your current hand");

        if (currentTrick.isEmpty()) {
            // Nothing else to verify, you can lead whatever you want from your hand
            return;
        }

        verifyFollowingSuit(hand, card, currentTrick);
        if (card instanceof Trump) {
            verifyHigherTrump(hand, (Trump) card, currentTrick);
        }
    }

    private static void verifyFollowingSuit(Hand hand, Card card, List<Play> currentTrick) {
        if (currentTrick.isEmpty() || card == Trump.FOOL) {
            return;
        } else if (currentTrick.iterator().next().getCard() == Trump.FOOL) {
            verifyFollowingSuit(hand, card, currentTrick.subList(1, currentTrick.size()));
        } else {
            Card nonFoolFirstCard = currentTrick.iterator().next().getCard();
            Optional<Suit> trickSuit = Cards.getSuit(nonFoolFirstCard);
            Optional<Suit> cardSuit = Cards.getSuit(card);
            if (cardSuit.equals(trickSuit)) {
                return;
            } else if (trickSuit.isPresent()) {
                Verify.verify(
                        hand.getCards().stream().noneMatch(c -> Cards.getSuit(c).equals(trickSuit)),
                        "Player attempted to play %s during %s trick despite having %s cards.",
                        card,
                        trickSuit.get(),
                        trickSuit.get());
            } else {
                Verify.verify(
                        hand.getCards().stream().allMatch(c -> c instanceof Suited || c == Trump.FOOL),
                        "Player attempted to play %s during trump trick despite having non-fool trump.",
                        card);
            }
        }
    }

    private static void verifyHigherTrump(Hand hand, Trump card, List<Play> currentTrick) {
        if (card == Trump.FOOL) {
            // You can always play the fool
            return;
        }
        Optional<Trump> maxPlayedTrump = getMaxTrump(currentTrick);
        if (maxPlayedTrump.isPresent() && Trump.ordering.compare(card, maxPlayedTrump.get()) < 0) {
            Verify.verify(
                    hand.getCards().stream()
                            .filter(c -> c instanceof Trump)
                            .allMatch(c -> Trump.ordering.compare((Trump) c, maxPlayedTrump.get()) < 0),
                    "Must play a higher trump when possible");
        }
    }

    public static Optional<Trump> getMaxTrump(List<Play> currentTrick) {
        List<Trump> trump = currentTrick.stream()
                .filter(p -> p.getCard() instanceof Trump)
                .map(p -> (Trump) p.getCard())
                .collect(Collectors.toList());
        if (trump.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(Trump.ordering.max(trump.iterator()));
        }
    }


    public static Play getWinner(Trick trick) {
        if (trick.getPlays().iterator().next().getCard() == Trump.FOOL) {
            return getWinnerWithFoolLead(trick);
        } else {
            return getWinnerWithNonFoolLead(trick);
        }
    }

    private static Play getWinnerWithFoolLead(Trick trick) {
        Play firstPlay = trick.getPlays().iterator().next();
        if (trick.getPlayersWithStrongFool().contains(firstPlay.getPlayer())) {
            return firstPlay;
        } else {
            // weak fool. We can pretend the trick doesn't involve the first play when computing the winner
            Iterator<Play> plays = trick.getPlays().iterator();
            // consume the fool
            plays.next();
            return getWinnerWithNonFoolLead(plays, ImmutableSet.of());
        }
    }

    private static Play getWinnerWithNonFoolLead(Trick trick) {
        return getWinnerWithNonFoolLead(trick.getPlays().iterator(), trick.getPlayersWithStrongFool());
    }

    private static Play getWinnerWithNonFoolLead(Iterator<Play> plays, Set<String> playersWithStrongFool) {
        Play winner = plays.next();
        Suit trickSuit = null;
        if (winner.getCard() instanceof Suited) {
            trickSuit = ((Suited) winner.getCard()).getSuit();
        }
        while (plays.hasNext()) {
            Play curr = plays.next();
            if (curr.getCard() == Trump.FOOL) {
                if (playersWithStrongFool.contains(curr.getPlayer())) {
                    winner = curr;
                }
                continue;
            }

            if (curr.getCard() instanceof Suited && winner.getCard() instanceof Trump) {
                continue;
            } else if (curr.getCard() instanceof Trump && winner.getCard() instanceof Suited) {
                winner = curr;
            } else if (curr.getCard() instanceof Suited) {
                Suited suitedCurr = (Suited) curr.getCard();
                Suited suitedWinner = (Suited) winner.getCard();
                if (suitedCurr.getSuit() == trickSuit && suitedCurr.getValue() > suitedWinner.getValue()) {
                    winner = curr;
                }
                // else it's off-suit and irrelevant
            } else {
                // they're both non-fool trump
                winner = Trump.ordering.<Play>onResultOf(play -> (Trump) play.getCard()).max(curr, winner);
            }
        }
        return winner;
    }
}
