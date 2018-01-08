package tarot;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import tarot.game.Round;
import tarot.game.Round.Result;
import tarot.game.Scorer.PalantirScorer;
import tarot.state.Deck;

public class Tarot {
    public static void main(String[] args) {
        List<String> playerIds = ImmutableList.of("n", "e", "s", "w", "nw");
        Deck deck = Deck.unshuffled();
        for (int i = 0; i < 100000; i++) {
            Round round = Round.create(playerIds, deck, new PalantirScorer());
            Optional<Result> result = round.play();
            System.out.println(result);
        }
    }
}
