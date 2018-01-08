package tarot.game;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import tarot.game.Scorer.PalantirScorer;
import tarot.state.Bid;
import tarot.state.Card.Suited;
import tarot.state.Card.Trump;

public class PalantirScorerTest {
    @Test
    public void testSimple() {
        PalantirScorer scorer = new PalantirScorer();
        List<Trick> tricks = ImmutableList.of(
                ImmutableTrick.builder()
                        .numberInRound(1)
                        .addPlays(
                                ImmutablePlay.of("nw", Trump.TWO),
                                ImmutablePlay.of("w", Trump.NINE),
                                ImmutablePlay.of("s", Trump.ELEVEN),
                                ImmutablePlay.of("e", Trump.TWENTY_ONE),
                                ImmutablePlay.of("n", Trump.SEVEN))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(2)
                        .addPlays(
                                ImmutablePlay.of("e", Suited.D_D),
                                ImmutablePlay.of("n", Suited.D_R),
                                ImmutablePlay.of("nw", Suited.D_1),
                                ImmutablePlay.of("w", Suited.D_4),
                                ImmutablePlay.of("s", Suited.D_3))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(3)
                        .addPlays(
                                ImmutablePlay.of("n", Suited.H_1),
                                ImmutablePlay.of("nw", Suited.H_7),
                                ImmutablePlay.of("w", Suited.H_3),
                                ImmutablePlay.of("s", Suited.H_8),
                                ImmutablePlay.of("e", Trump.FOOL))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(4)
                        .addPlays(
                                ImmutablePlay.of("s", Suited.C_R),
                                ImmutablePlay.of("e", Suited.C_6),
                                ImmutablePlay.of("n", Suited.C_2),
                                ImmutablePlay.of("nw", Suited.C_D),
                                ImmutablePlay.of("w", Suited.C_8))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(5)
                        .addPlays(
                                ImmutablePlay.of("s", Suited.C_1),
                                ImmutablePlay.of("e", Trump.SIX),
                                ImmutablePlay.of("n", Suited.C_V),
                                ImmutablePlay.of("nw", Suited.C_4),
                                ImmutablePlay.of("w", Suited.C_9))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(6)
                        .addPlays(
                                ImmutablePlay.of("e", Suited.D_2),
                                ImmutablePlay.of("n", Suited.D_5),
                                ImmutablePlay.of("nw", Trump.THREE),
                                ImmutablePlay.of("w", Suited.D_6),
                                ImmutablePlay.of("s", Suited.D_10))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(7)
                        .addPlays(
                                ImmutablePlay.of("nw", Trump.TWELVE),
                                ImmutablePlay.of("w", Trump.TWENTY),
                                ImmutablePlay.of("s", Trump.EIGHTEEN),
                                ImmutablePlay.of("e", Trump.EIGHT),
                                ImmutablePlay.of("n", Trump.TEN))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(8)
                        .addPlays(
                                ImmutablePlay.of("w", Trump.ONE),
                                ImmutablePlay.of("s", Suited.S_D),
                                ImmutablePlay.of("e", Trump.SEVENTEEN),
                                ImmutablePlay.of("n", Suited.H_2),
                                ImmutablePlay.of("nw", Trump.NINETEEN))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(9)
                        .addPlays(
                                ImmutablePlay.of("nw", Trump.FIVE),
                                ImmutablePlay.of("w", Suited.H_4),
                                ImmutablePlay.of("s", Suited.C_3),
                                ImmutablePlay.of("e", Trump.FOURTEEN),
                                ImmutablePlay.of("n", Suited.D_V))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(10)
                        .addPlays(
                                ImmutablePlay.of("e", Suited.D_7),
                                ImmutablePlay.of("n", Suited.S_4),
                                ImmutablePlay.of("nw", Trump.FOUR),
                                ImmutablePlay.of("w", Suited.D_9),
                                ImmutablePlay.of("s", Suited.S_C))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(11)
                        .addPlays(
                                ImmutablePlay.of("nw", Trump.THIRTEEN),
                                ImmutablePlay.of("w", Suited.H_5),
                                ImmutablePlay.of("s", Suited.S_3),
                                ImmutablePlay.of("e", Trump.SIXTEEN),
                                ImmutablePlay.of("n", Suited.C_7))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(12)
                        .addPlays(
                                ImmutablePlay.of("e", Suited.D_8),
                                ImmutablePlay.of("n", Suited.S_8),
                                ImmutablePlay.of("nw", Trump.FIFTEEN),
                                ImmutablePlay.of("w", Suited.S_6),
                                ImmutablePlay.of("s", Suited.S_V))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(13)
                        .addPlays(
                                ImmutablePlay.of("nw", Suited.S_5),
                                ImmutablePlay.of("w", Suited.S_7),
                                ImmutablePlay.of("s", Suited.H_6),
                                ImmutablePlay.of("e", Suited.S_1),
                                ImmutablePlay.of("n", Suited.S_9))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(14)
                        .addPlays(
                                ImmutablePlay.of("n", Suited.S_R),
                                ImmutablePlay.of("nw", Suited.S_10),
                                ImmutablePlay.of("w", Suited.H_9),
                                ImmutablePlay.of("s", Suited.C_5),
                                ImmutablePlay.of("e", Suited.S_2))
                        .build(),
                ImmutableTrick.builder()
                        .numberInRound(15)
                        .addPlays(
                                ImmutablePlay.of("n", Suited.H_10),
                                ImmutablePlay.of("nw", Suited.H_V),
                                ImmutablePlay.of("w", Suited.C_10),
                                ImmutablePlay.of("s", Suited.H_R),
                                ImmutablePlay.of("e", Suited.D_C))
                        .build());

        float result = scorer.computeHandScore(
                Bid.SMALL,
                ImmutableSet.of(),
                tricks,
                ImmutableSet.of("e", "n"),
                ImmutableSet.of("nw", "w", "s"),
                ImmutableSet.of(Suited.H_C, Suited.H_D, Suited.C_C));
        assertThat(result, is(20.0f));
    }
}