package tarot.state;

import com.google.common.collect.Ordering;

public enum Bid {
    SMALL,
    PUSH,
    GUARD,
    GUARD_WITHOUT,
    GUARD_AGAINST;

    // Use an explicit comparator instead of depending on ordinal like Enum.compareTo() does
    private static final Ordering<Bid> ordering = Ordering.explicit(SMALL, PUSH, GUARD, GUARD_WITHOUT, GUARD_AGAINST);

    public boolean isGreaterThan(Bid bid) {
        return ordering.compare(this, bid) > 0;
    }
}
