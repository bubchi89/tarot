package tarot.game;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import tarot.game.Trick.Play;
import tarot.state.Bid;
import tarot.state.Card;
import tarot.state.Card.Trump;
import tarot.state.Cards;

public interface Scorer {
    float computeHandScore(Bid bid,
                           Set<Card> handfulShown,
                           List<Trick> tricks,
                           Set<String> attackers,
                           Set<String> defenders,
                           Set<Card> aside);

    class PalantirScorer implements Scorer {
        private static final Logger log = LoggerFactory.getLogger(PalantirScorer.class);

        @Override
        public float computeHandScore(Bid bid,
                                      Set<Card> handfulShown,
                                      List<Trick> tricks,
                                      Set<String> attackers,
                                      Set<String> defenders,
                                      Set<Card> aside) {
            log.debug(
                    "Computing hand score.\n\tbid: {}\n\thandful: {}\n\ttricks: {}\n\tattackers: {}" +
                            "\n\tdefenders: {}\n\taside: {}",
                    bid,
                    handfulShown,
                    tricks,
                    attackers,
                    defenders,
                    aside);

            Set<Card> attackerCards = new HashSet<>();
            Set<Card> defenderCards = new HashSet<>();
            int numAttackerTricks = 0;
            boolean attackersOwedPoint = false;
            boolean defendersOwedPoint = false;

            for (int i = 0; i < tricks.size() - 1; i++) {
                Trick trick = tricks.get(i);
                if (defenders.contains(trick.getWinner().getPlayer())) {
                    for (Play play : trick.getPlays()) {
                        if (play.getCard() == Trump.FOOL && attackers.contains(play.getPlayer())) {
                            attackerCards.add(play.getCard());
                            attackersOwedPoint = true;
                        } else {
                            defenderCards.add(play.getCard());
                        }
                    }
                } else {
                    for (Play play : trick.getPlays()) {
                        if (play.getCard() == Trump.FOOL && defenders.contains(play.getPlayer())) {
                            defenderCards.add(play.getCard());
                            defendersOwedPoint = true;
                        } else {
                            attackerCards.add(play.getCard());
                        }
                    }
                    numAttackerTricks++;
                }
            }
            Trick lastTrick = Iterables.getLast(tricks);
            for (Play play : lastTrick.getPlays()) {
                boolean defendersWon = defenders.contains(lastTrick.getWinner().getPlayer());
                if (play.getCard() != Trump.FOOL) {
                    if (!defendersWon) {
                        attackerCards.add(play.getCard());
                    } else {
                        defenderCards.add(play.getCard());
                    }
                } else {
                    if (numAttackerTricks == Trick.TRICKS_PER_ROUND - 1) {
                        // if the defenders played it, then they lost it. If an attacker played it, then it was strong
                        // and won the trick
                        attackerCards.add(play.getCard());
                    } else if (numAttackerTricks == 0) {
                        // the opposite of the previous case
                        defenderCards.add(play.getCard());
                    } else {
                        // it was lost by whoever played it
                        if (defenders.contains(play.getPlayer())) {
                            attackerCards.add(play.getCard());
                            if (defendersWon) {
                                defendersOwedPoint = true;
                            }
                        } else {
                            defenderCards.add(play.getCard());
                            if (!defendersWon) {
                                attackersOwedPoint = true;
                            }
                        }
                    }
                }

                if (!defendersWon) {
                    numAttackerTricks++;
                }
            }
            log.debug("attacker cards: {}\ndefender cards: {}", attackerCards, defenderCards);

            // Double to avoid rounding errors. Although half points should be representable exactly in a float, I think
            int actualDoublePoints =
                    computeDoublePoints(attackerCards) +
                    computeDoubleAsidePoints(bid, aside) +
                    computeDoubleFoolAdjustment(attackerCards, defenderCards, attackersOwedPoint, defendersOwedPoint);
            int targetDoublePoints = 2 * computeTargetPoints(attackerCards);
            boolean madeContract = actualDoublePoints >= targetDoublePoints;
            int bonusPoints =
                    computePointDifferenceBonus(actualDoublePoints, targetDoublePoints) +
                    computePetitAuBoutPoints(madeContract, numAttackerTricks, tricks, attackers, defenders) +
                    computeHandfulPoints(handfulShown);
            int finalScore = getScoreForBid(bid) + bonusPoints;

            // TODO: slam
            if (madeContract) {
                return (float) finalScore;
            } else {
                return (float) -finalScore;
            }
        }

        private static int computeDoubleAsidePoints(Bid bid, Set<Card> aside) {
            switch (bid) {
            case SMALL:
                // fall through
            case PUSH:
                // fall through
            case GUARD:
                // fall through
            case GUARD_WITHOUT:
                return computeDoublePoints(aside);
            case GUARD_AGAINST:
                return -computeDoublePoints(aside);
            default:
                throw new IllegalArgumentException("Unknown bid: " + bid);
            }
        }

        private static int computeDoublePoints(Set<Card> cards) {
            return cards.stream().map(Cards::getDoublePoints).reduce((a, b) -> a + b).orElse(0);
        }

        private static int computeDoubleFoolAdjustment(Set<Card> attackerCards,
                                                       Set<Card> defenderCards,
                                                       boolean attackersOwedPoint,
                                                       boolean defendersOwedPoint) {
            if (attackersOwedPoint && defenderCards.stream().anyMatch(c -> Cards.getDoublePoints(c) == 1)) {
                return 1;
            } else if (defendersOwedPoint &&
                    attackerCards.stream().anyMatch(c -> Cards.getDoublePoints(c) == 1)) {
                return -1;
            } else {
                return 0;
            }
        }

        private static int computeTargetPoints(Set<Card> cards) {
            int numBouts = Sets.intersection(Cards.getBouts(), cards).size();
            if (numBouts == 0) {
                return 56;
            } else if (numBouts == 1) {
                return 51;
            } else if (numBouts == 2) {
                return 41;
            } else if (numBouts == 3) {
                return 36;
            } else {
                throw new AssertionError("Unexpected number of bouts won: " + numBouts);
            }
        }

        /**
         * The difference between the target score and the actual score rounded up to a 10 point multiple.
         * Examples (note that in the actual calculation, all of the input values are doubled):
         *   51/51 -> 0
         *   51.5/51 -> 10
         *   61/51 -> 10
         *   62/51 -> 20
         *   50.5/51 -> 10 (this is later negated)
         *   41.5/51 -> 10 (this is later negated)
         */
        private static int computePointDifferenceBonus(int actualDoublePoints, int targetDoublePoints) {
            int bonus = ((Math.abs(targetDoublePoints - actualDoublePoints) + 19) / 20) * 10;
            log.debug("{}/{}, point difference bonus: {}", actualDoublePoints / 2.0f, targetDoublePoints / 2.0f, bonus);
            return bonus;
        }

        private static int computePetitAuBoutPoints(boolean madeContract,
                                                    int numTakerAndPartnerTricks,
                                                    List<Trick> tricks,
                                                    Set<String> attackers,
                                                    Set<String> defenders) {
            Optional<String> petitAuBoutPlayer =
                    getPetitAuBoutPlayer(numTakerAndPartnerTricks, tricks, attackers, defenders);
            if (petitAuBoutPlayer.isPresent()) {
                if (defenders.contains(petitAuBoutPlayer.get())) {
                    if (madeContract) {
                        return -10;
                    } else {
                        return 10;
                    }
                } else {
                    if (madeContract) {
                        return 10;
                    } else {
                        return -10;
                    }
                }
            } else {
                return 0;
            }
        }

        private static Optional<String> getPetitAuBoutPlayer(int numTakerAndPartnerTricks,
                                                             List<Trick> tricks,
                                                             Set<String> attackers,
                                                             Set<String> defenders) {
            if (numTakerAndPartnerTricks == Trick.TRICKS_PER_ROUND) {
                // slam. Check for '1' play followed by the fool in the last round
                Optional<String> petitAuBoutPlayer = tricks.get(Trick.TRICKS_PER_ROUND - 2).getPlays().stream()
                        .filter(pac -> pac.getCard() == Trump.ONE && attackers.contains(pac.getPlayer()))
                        .map(Play::getPlayer)
                        .findAny();
                boolean trumpPlayedLast = Iterables.getLast(tricks).getPlays().stream()
                        .anyMatch(pac -> pac.getCard() == Trump.FOOL && attackers.contains(pac.getPlayer()));
                if (petitAuBoutPlayer.isPresent() && trumpPlayedLast) {
                    return petitAuBoutPlayer;
                }
                // fall-through to the standard petit au bout check
            } else if (numTakerAndPartnerTricks == 0) {
                // anti-slam. Check for '1' play followed by the fool in the last round
                Optional<String> petitAuBoutPlayer = tricks.get(Trick.TRICKS_PER_ROUND - 2).getPlays().stream()
                        .filter(pac -> pac.getCard() == Trump.ONE && defenders.contains(pac.getPlayer()))
                        .map(Play::getPlayer)
                        .findAny();
                boolean trumpPlayedLast = Iterables.getLast(tricks).getPlays().stream()
                        .anyMatch(pac -> pac.getCard() == Trump.FOOL && defenders.contains(pac.getPlayer()));
                if (petitAuBoutPlayer.isPresent() && trumpPlayedLast) {
                    return petitAuBoutPlayer;
                }
                // fall-through to the standard petit au bout check
            }

            // typical case. one at the end
            return Iterables.getLast(tricks).getPlays().stream()
                    .filter(pac -> pac.getCard() == Trump.ONE)
                    .map(Play::getPlayer)
                    .findAny();
        }

        // All handfuls are treated the same
        private static int computeHandfulPoints(Set<Card> handful) {
            if (handful.size() > 0) {
                return 10;
            } else {
                return 0;
            }
        }

        private int getScoreForBid(Bid bid) {
            switch (bid) {
            case SMALL:
                return 10;
            case PUSH:
                return 20;
            case GUARD:
                return 40;
            case GUARD_WITHOUT:
                return 80;
            case GUARD_AGAINST:
                return 160;
            default:
                throw new AssertionError("Unknown bid: " + bid);
            }
        }
    }
}
