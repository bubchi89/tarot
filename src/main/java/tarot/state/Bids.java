package tarot.state;

public class Bids {
    private Bids() {
        // Prevent instantiation
    }

    public static boolean canSeeDog(Bid bid) {
        return bid == Bid.SMALL || bid == Bid.PUSH || bid == Bid.GUARD;
    }
}
