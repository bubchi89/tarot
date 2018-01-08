package tarot.ai;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;

import tarot.game.Bidding;
import tarot.state.Card;
import tarot.state.Card.Trump;
import tarot.state.Cards;
import tarot.state.Hand;

public class RandomDogStrategy extends AbstractDogStrategy {
    @Override
    protected Set<Card> chooseAsideInner(Hand hand, Set<Card> dog, Bidding bidding, @Nullable Card partnerCard) {
        List<Card> nonTrump = Streams.concat(hand.getCards().stream(), dog.stream())
                .filter(c -> !(c instanceof Trump))
                .collect(Collectors.toList());
        Collections.shuffle(nonTrump);
        List<Card> randomNonTrumpOrKing = nonTrump.subList(0, Math.min(nonTrump.size(), dog.size()));
        if (randomNonTrumpOrKing.size() >= dog.size()) {
            return ImmutableSet.copyOf(randomNonTrumpOrKing);
        }

        Set<Card> bouts = Cards.getBouts();
        List<Card> nonBoutTrump = Streams.concat(hand.getCards().stream(), dog.stream())
                .filter(c -> c instanceof Trump)
                .filter(c -> !bouts.contains(c))
                .collect(Collectors.toList());
        Collections.shuffle(nonBoutTrump);
        List<Card> randomNonBoutTrump = nonBoutTrump.subList(0, dog.size() - randomNonTrumpOrKing.size());
        return ImmutableSet.<Card>builder().addAll(randomNonTrumpOrKing).addAll(randomNonBoutTrump).build();
    }
}
