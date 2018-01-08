package tarot.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Verify;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import tarot.state.Bid;

public class Bidding {
    private static final Logger log = LoggerFactory.getLogger(Bidding.class);

    private final List<BidAndBidder> bidSequence = new ArrayList<>();
    private final Map<String, Bid> maxBids = new HashMap<>();

    public static Bidding newBidding() {
        return new Bidding();
    }

    public Optional<Bid> getBid() {
        return Optional.ofNullable(getCurrMaxBidAndBidder()).map(BidAndBidder::getBid);
    }

    public Optional<Bidder> getTaker() {
        return Optional.ofNullable(getCurrMaxBidAndBidder()).map(BidAndBidder::getBidder);
    }

    public List<BidAndBidder> getBidSequence() {
        return bidSequence;
    }

    public Map<String, Bid> getMaxBids() {
        return maxBids;
    }

    public void run(List<? extends Bidder> players) {
        // Note that 'players' is already sorted, and the first player of the list will be at the head of the Iterator
        // Copy so that we don't mess up the original list
        Iterator<? extends Bidder> bidders = Iterators.cycle(new ArrayList<>(players));
        while (bidders.hasNext()) {
            Bidder currBidder = bidders.next();
            BidAndBidder currMax = getCurrMaxBidAndBidder();
            if (currMax != null && currMax.getBidder().equals(currBidder)) {
                // Only one bidder left that already bid (i.e. you can't raise yourself)
                log.debug("'{}' wins bid ({})", currBidder.getId(), getBid());
                break;
            } else {
                Optional<Bid> bid = currBidder.bid(this);
                if (bid.isPresent()) {
                    log.debug("{}: bid '{}'", currBidder.getId(), bid.get());
                    addBid(currBidder, bid.get());
                } else {
                    log.debug("{}: pass", currBidder.getId());
                    bidders.remove();
                }
            }
        }
    }

    @Nullable
    private BidAndBidder getCurrMaxBidAndBidder() {
        return Iterables.getLast(bidSequence, null);
    }

    private void addBid(Bidder bidder, Bid newBid) {
        Optional<Bid> currMax = getBid();
        Verify.verify(
                !currMax.isPresent() || newBid.isGreaterThan(currMax.get()),
                "Player attempted to bid %s, but the current bid is already %s",
                newBid,
                currMax);
        bidSequence.add(ImmutableBidAndBidder.of(bidder, newBid));
        maxBids.put(bidder.getId(), newBid);
    }

    @Value.Immutable
    public interface BidAndBidder {
        @Value.Parameter
        Bidder getBidder();

        @Value.Parameter
        Bid getBid();
    }

    interface Bidder {
        String getId();

        Optional<Bid> bid(Bidding state);
    }
}
