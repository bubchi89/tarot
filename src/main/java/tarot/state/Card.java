package tarot.state;

import com.google.common.collect.Ordering;

public interface Card {
    enum Suited implements Card {
        C_R(Suit.SPADE, 14),
        C_D(Suit.SPADE, 13),
        C_C(Suit.SPADE, 12),
        C_V(Suit.SPADE, 11),
        C_10(Suit.SPADE, 10),
        C_9(Suit.SPADE, 9),
        C_8(Suit.SPADE, 8),
        C_7(Suit.SPADE, 7),
        C_6(Suit.SPADE, 6),
        C_5(Suit.SPADE, 5),
        C_4(Suit.SPADE, 4),
        C_3(Suit.SPADE, 3),
        C_2(Suit.SPADE, 2),
        C_1(Suit.SPADE, 1),
        D_R(Suit.DIAMOND, 14),
        D_D(Suit.DIAMOND, 13),
        D_C(Suit.DIAMOND, 12),
        D_V(Suit.DIAMOND, 11),
        D_10(Suit.DIAMOND, 10),
        D_9(Suit.DIAMOND, 9),
        D_8(Suit.DIAMOND, 8),
        D_7(Suit.DIAMOND, 7),
        D_6(Suit.DIAMOND, 6),
        D_5(Suit.DIAMOND, 5),
        D_4(Suit.DIAMOND, 4),
        D_3(Suit.DIAMOND, 3),
        D_2(Suit.DIAMOND, 2),
        D_1(Suit.DIAMOND, 1),
        H_R(Suit.HEART, 14),
        H_D(Suit.HEART, 13),
        H_C(Suit.HEART, 12),
        H_V(Suit.HEART, 11),
        H_10(Suit.HEART, 10),
        H_9(Suit.HEART, 9),
        H_8(Suit.HEART, 8),
        H_7(Suit.HEART, 7),
        H_6(Suit.HEART, 6),
        H_5(Suit.HEART, 5),
        H_4(Suit.HEART, 4),
        H_3(Suit.HEART, 3),
        H_2(Suit.HEART, 2),
        H_1(Suit.HEART, 1),
        S_R(Suit.SPADE, 14),
        S_D(Suit.SPADE, 13),
        S_C(Suit.SPADE, 12),
        S_V(Suit.SPADE, 11),
        S_10(Suit.SPADE, 10),
        S_9(Suit.SPADE, 9),
        S_8(Suit.SPADE, 8),
        S_7(Suit.SPADE, 7),
        S_6(Suit.SPADE, 6),
        S_5(Suit.SPADE, 5),
        S_4(Suit.SPADE, 4),
        S_3(Suit.SPADE, 3),
        S_2(Suit.SPADE, 2),
        S_1(Suit.SPADE, 1);

        private final Suit suit;
        private final int value;

        Suited(Suit suit, int value) {
            this.suit = suit;
            this.value = value;
        }

        public Suit getSuit() {
            return suit;
        }

        public int getValue() {
            return value;
        }
    }

    enum Trump implements Card {
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
        TEN,
        ELEVEN,
        TWELVE,
        THIRTEEN,
        FOURTEEN,
        FIFTEEN,
        SIXTEEN,
        SEVENTEEN,
        EIGHTEEN,
        NINETEEN,
        TWENTY,
        TWENTY_ONE,
        FOOL;

        public static final Ordering<Trump> ordering = Ordering.explicit(
                FOOL,
                ONE,
                TWO,
                THREE,
                FOUR,
                FIVE,
                SIX,
                SEVEN,
                EIGHT,
                NINE,
                TEN,
                ELEVEN,
                TWELVE,
                THIRTEEN,
                FOURTEEN,
                FIFTEEN,
                SIXTEEN,
                SEVENTEEN,
                EIGHTEEN,
                NINETEEN,
                TWENTY,
                TWENTY_ONE);
    }
}
