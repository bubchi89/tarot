package tarot.ai;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import tarot.game.Bidding;
import tarot.game.Trick.Play;
import tarot.state.Card;
import tarot.state.Hand;

public abstract class AbstractTrickStrategy<T> implements TrickStrategy {
    @Nullable
    private T roundState;

    @Override
    public Card pickCard(List<Play> currentTrick,
                         Hand hand,
                         Bidding bidding,
                         List<String> playerIds,
                         String takerId,
                         @Nullable Card partnerCard) {
        Result<T> result = pickCardInner(currentTrick, hand, bidding, playerIds, takerId, partnerCard, roundState);
        roundState = result.getRoundState();
        return result.getPlay();
    }

    protected abstract Result<T> pickCardInner(List<Play> currentTrick,
                                               Hand hand,
                                               Bidding bidding,
                                               List<String> playerIds,
                                               String takerId,
                                               Card partnerCard,
                                               @Nullable T roundState);

    @Value.Immutable
    interface Result<T> {
        Card getPlay();

        T getRoundState();
    }
}
