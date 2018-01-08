package tarot.ai;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import tarot.game.Bidding;
import tarot.state.Bid;
import tarot.state.Hand;

public interface BiddingStrategy {
    Optional<Bid> bid(Hand hand, Bidding bidding);

    class Random implements BiddingStrategy {
        private static final java.util.Random rng = new java.util.Random();

        @Override
        public Optional<Bid> bid(Hand hand, Bidding bidding) {
            Bid minBid = getMinBid(bidding);
            List<Bid> candidateBids =
                    EnumSet.allOf(Bid.class).stream().filter(b -> b.isGreaterThan(minBid)).collect(Collectors.toList());
            int index = rng.nextInt(candidateBids.size() + 1);
            if (index < candidateBids.size()) {
                return Optional.of(candidateBids.get(index));
            } else {
                return Optional.empty();
            }
        }

        private static Bid getMinBid(Bidding bidding) {
            return bidding.getBid().orElse(Bid.SMALL);
        }
    }
}
