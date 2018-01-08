package tarot.game;

import java.util.List;
import java.util.Set;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import tarot.state.Card;

@Value.Immutable
public interface Trick {
    // TODO: (rliu) A fair amount of code is agnostic to player size. But a lot of it isn't (e.g. anything involving a
    // partner...)
    int TRICKS_PER_ROUND = 15;

    int getNumberInRound();

    List<Play> getPlays();

    /**
     * If one side has won every trick so far and {@link #getNumberInRound()} == {@link #TRICKS_PER_ROUND}, this
     * returns the players on that side. Otherwise it returns the empty set.
     */
    @Value.Default
    default Set<String> getPlayersWithStrongFool() {
        return ImmutableSet.of();
    }

    /**
     * @return {@link Tricks#getWinner(Trick)} using 'this'
     */
    @Value.Lazy
    default Play getWinner() {
        return Tricks.getWinner(this);
    }

    @Value.Check
    default void checkNumberInRound() {
        Range<Integer> validRange = Range.closed(1, TRICKS_PER_ROUND);
        Preconditions.checkState(
                validRange.contains(getNumberInRound()),
                "Number of trick in round must be in %s",
                validRange);
    }

    @Value.Check
    default void checkDuplicateCard() {
        int uniqueCards = (int) getPlays().stream().map(Play::getCard).distinct().count();
        Preconditions.checkState(uniqueCards == getPlays().size(), "All cards in a trick should be distinct");
    }

    @Value.Check
    default void checkDuplicatePlayer() {
        int uniquePlayers = (int) getPlays().stream().map(Play::getPlayer).distinct().count();
        Preconditions.checkState(uniquePlayers == getPlays().size(), "All players in a trick should be distinct");
    }

    @Value.Check
    default void checkStrongFool() {
        Preconditions.checkState(
                getPlayersWithStrongFool().isEmpty() || getNumberInRound() == TRICKS_PER_ROUND,
                "The fool can only be strong on the last trick");
    }

    @Value.Immutable
    interface Play {
        @Value.Parameter
        String getPlayer();

        @Value.Parameter
        Card getCard();
    }
}
