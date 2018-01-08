package tarot.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import tarot.ai.BiddingStrategy;
import tarot.ai.DogStrategy;
import tarot.ai.PartnerStrategy;
import tarot.ai.RandomDogStrategy;
import tarot.ai.RandomPartnerStrategy;
import tarot.ai.RandomTrickStrategy;
import tarot.ai.TrickStrategy;
import tarot.game.Bidding.Bidder;
import tarot.game.Round.SetupState.PlayerState;
import tarot.game.Trick.Play;
import tarot.state.Bid;
import tarot.state.Bids;
import tarot.state.Card;
import tarot.state.Card.Trump;
import tarot.state.Deck;
import tarot.state.Deck.Deal;
import tarot.state.Hand;
import tarot.state.ImmutableDeal;
import tarot.state.ImmutableHand;

public class Round {
    private static final Logger log = LoggerFactory.getLogger(Round.class);

    private static final Random rng = new Random();

    private final List<String> playerIds;
    private final Deck deck;
    private final Scorer scorer;

    private Round(List<String> playerIds, Deck deck, Scorer scorer) {
        this.playerIds = playerIds;
        this.deck = deck;
        this.scorer = scorer;
    }

    public static Round create(List<String> playerIds, Deck deck, Scorer scorer) {
        Preconditions.checkArgument(
                playerIds.stream().distinct().count() == 5,
                "Only five player mode is supported right now. All ids should be unique. Input: %s",
                playerIds);
        return new Round(playerIds, deck, scorer);
    }

    public Optional<Result> play() {
        Optional<SetupState> setupResult = setup();
        if (!setupResult.isPresent()) {
            return Optional.empty();
        } else {
            return playAfterSetup(setupResult.get());
        }
    }

    private Optional<Result> playAfterSetup(SetupState setup) {
        PlayerState playerState = setup.getPlayerState();

        Set<Card> handful = checkHandful(
                playerState.getTaker(),
                setup.getBidding(),
                Players.getIds(playerState.getPlayers()),
                ImmutableSet.copyOf(playerState.getPlayers()));
        List<Trick> tricks = playTricks(setup, !handful.isEmpty());
        Map<Player, Float> score = score(tricks, setup, handful);

        return Optional.of(ImmutableResult.builder()
                .bidding(setup.getBidding())
                .firstPlayer(playerIds.iterator().next())
                .taker(playerState.getTaker().getId())
                .partner(playerState.getPartner().getId())
                .partnerCard(setup.getPartnerCard())
                .dog(setup.getDeal().getDog())
                .aside(setup.getAside())
                .tricks(tricks)
                .score(score.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getId(), e -> e.getValue())))
                .build());
    }

    private Optional<SetupState> setup() {
        Deal deal = softShuffleAndDeal();

        List<Player> players = initializePlayers(deal);

        Bidding bidding = bid(players);
        if (!bidding.getBid().isPresent()) {
            // Round ended without a successful bid
            return Optional.empty();
        }

        String takerId = bidding.getTaker().get().getId();
        Player taker = players.stream().filter(p -> p.getId().equals(takerId)).findFirst().get();

        Optional<Card> partnerCard = taker.callPartnerIfPossible(bidding);
        Player partner = computePartner(players, partnerCard.orElse(null)).orElse(taker);

        Set<Card> aside = handleDog(deal, bidding, taker, partnerCard.orElse(null));

        return Optional.of(ImmutableSetupState.builder()
                .deal(deal)
                .bidding(bidding)
                .partnerCard(partnerCard)
                .playerState(ImmutablePlayerState.builder().players(players).taker(taker).partner(partner).build())
                .aside(aside)
                .build());
    }

    private Deal softShuffleAndDeal() {
        while (true) {
            deck.softShuffle();
            Deal potentialDeal = deal(deck);
            boolean isMaldonne = potentialDeal.getHands().stream().anyMatch(h -> {
                Set<Card> trumpCards = h.getCards().stream().filter(c -> c instanceof Trump).collect(Collectors.toSet());
                return trumpCards.equals(ImmutableSet.of(Trump.ONE));
            });
            if (!isMaldonne) {
                log.debug("deal: {}", potentialDeal);
                return potentialDeal;
            }
        }
    }

    private Deal deal(Deck deck) {
        List<ImmutableHand.Builder> hands =
                IntStream.range(0, 5).mapToObj(unused -> ImmutableHand.builder()).collect(Collectors.toList());
        Set<Card> dog = new HashSet<>(3);

        List<Card> asList = new ArrayList<>(deck.getCards());
        Iterator<ImmutableHand.Builder> currHand = Iterators.cycle(hands);
        for (int i = 0; i < asList.size(); /* done in loop */){
            if (shouldAddToDog(i, deck, dog.size())) {
                dog.add(asList.get(i));
                i++;
            } else {
                currHand.next().addCards(asList.get(i++));
                currHand.next().addCards(asList.get(i++));
                currHand.next().addCards(asList.get(i++));
            }
        }

        return ImmutableDeal.builder()
                .addAllHands(Collections2.transform(hands, ImmutableHand.Builder::build))
                .addAllDog(dog)
                .build();
    }

    private boolean shouldAddToDog(int deckIndex, Deck deck, int currDogSize) {
        if (currDogSize == 3) {
            // Already done
            return false;
        } else if (deckIndex == 0) {
            // Never deal the first card to the dog
            return false;
        } else {
            // Including the current one
            int numCardsRemaining = deck.getCards().size() - deckIndex;
            int numDogCardsRemaining = 3 - currDogSize;
            // We can't deal to the dog more than once and we can't deal to the dog last, so if we hit this bound we
            // _need_ to deal to the dog
            if (numCardsRemaining <= numDogCardsRemaining * 4) {
                return true;
            } else {
                return rng.nextBoolean();
            }
        }
    }

    private List<Player> initializePlayers(Deal deal) {
        Iterator<Hand> handsIter = deal.getHands().iterator();
        return playerIds.stream()
                .map(id -> Player.create(
                        id,
                        handsIter.next(),
                        new BiddingStrategy.Random(),
                        new RandomPartnerStrategy(),
                        new RandomDogStrategy(),
                        new RandomTrickStrategy()))
                .collect(Collectors.toList());
    }

    private Bidding bid(List<Player> players) {
        Bidding bidding = Bidding.newBidding();
        bidding.run(players);
        return bidding;
    }

    /**
     * @return The partner or {@link Optional#empty} if there is no partner (i.e. the taker called him/herself or could
     * not call a partner)
     */
    private static Optional<Player> computePartner(List<Player> players, @Nullable Card partnerCard) {
        if (partnerCard == null) {
            return Optional.empty();
        } else {
            return players.stream()
                    .filter(p -> p.getOriginalHand().getCards().contains(partnerCard))
                    .findAny();
        }
    }

    private Set<Card> handleDog(Deal deal, Bidding bidding, Player taker, @Nullable Card partnerCard) {
        if (Bids.canSeeDog(bidding.getBid().get())) {
            return taker.chooseAside(deal.getDog(), bidding, partnerCard);
            // TODO: handle if trumps are in aside which becomes public information!
        } else {
            return ImmutableSet.copyOf(deal.getDog());
        }
    }

    private static List<Trick> playTricks(SetupState setup, boolean handfulShown) {
        PlayerState playerState = setup.getPlayerState();

        List<String> playerIds = Players.getIds(playerState.getPlayers());
        Set<String> attackerIds = Players.getIds(playerState.getTaker(), playerState.getPartner());
        Set<String> defenderIds = Players.getIds(playerState.getPlayers().stream()
                .filter(p -> !p.equals(playerState.getTaker()) && !p.equals(playerState.getPartner()))
                .collect(Collectors.toSet()));
        AtomicReference<Boolean> handfulShownWrapped = new AtomicReference<>(handfulShown);

        List<Trick> tricks = new ArrayList<>();
        Player currPlayer = playerState.getPlayers().iterator().next();
        for (int numTrick = 0; numTrick < Trick.TRICKS_PER_ROUND; numTrick++) {
            Trick trick = playTrick(
                    currPlayer,
                    playerIds,
                    attackerIds,
                    defenderIds,
                    playerState.getTaker(),
                    playerState.getNextPlayers(),
                    setup.getBidding(),
                    setup.getPartnerCard().orElse(null),
                    handfulShownWrapped,
                    tricks);
            tricks.add(trick);
            currPlayer = playerState.getPlayer(trick.getWinner().getPlayer());
        }
        return tricks;
    }

    private static Trick playTrick(Player currPlayer,
                                   List<String> playerIds,
                                   Set<String> attackerIds,
                                   Set<String> defenderIds,
                                   Player taker,
                                   Map<Player, Player> nextPlayers,
                                   Bidding bidding,
                                   @Nullable Card partnerCard,
                                   AtomicReference<Boolean> handfulShown,
                                   @Output List<Trick> tricks) {
        boolean isFirstTrick = tricks.isEmpty();
        boolean hasTakerPlayed = !isFirstTrick;

        List<Play> currentPlays = new ArrayList<>(playerIds.size());
        for (int numPlayer = 0; numPlayer < playerIds.size(); numPlayer++) {
            if (isFirstTrick && !hasTakerPlayed && !handfulShown.get()) {
                Set<Card> shownCards = checkHandful(taker, bidding, playerIds, nextPlayers.keySet());
                if (!shownCards.isEmpty()) {
                    handfulShown.set(true);
                }
            }

            Card card = currPlayer.play(currentPlays, bidding, playerIds, taker.getId(), partnerCard);
            currentPlays.add(ImmutablePlay.of(currPlayer.getId(), card));

            if (!hasTakerPlayed && currPlayer.equals(taker)) {
                hasTakerPlayed = true;
            }
            currPlayer = nextPlayers.get(currPlayer);
        }
        int numberInRound = tricks.size() + 1;
        List<String> playersWithStrongFool =
                computePlayersWithStrongFool(numberInRound, attackerIds, defenderIds, tricks);
        return ImmutableTrick.builder()
                .numberInRound(numberInRound)
                .addAllPlays(currentPlays)
                .playersWithStrongFool(playersWithStrongFool)
                .build();
    }

    private static Set<Card> checkHandful(Player taker, Bidding bidding, List<String> playerIds, Set<Player> players) {
        Set<Card> shownCards = taker.checkHandful(bidding);
        if (!shownCards.isEmpty()) {
            for (Player player : players) {
                if (!player.equals(taker)) {
                    player.handleHandful(shownCards, bidding, playerIds);
                }
            }
        }
        return shownCards;
    }

    private static List<String> computePlayersWithStrongFool(int numberInRound,
                                                             Collection<String> attackerIds,
                                                             Collection<String> defenderIds,
                                                             List<Trick> tricks) {
        if (numberInRound == Trick.TRICKS_PER_ROUND) {
            if (tricks.stream().allMatch(t -> attackerIds.contains(t.getWinner().getPlayer()))) {
                return ImmutableList.copyOf(attackerIds);
            } else if (tricks.stream().allMatch(t -> defenderIds.contains(t.getWinner().getPlayer()))) {
                return ImmutableList.copyOf(defenderIds);
            }
            // fall through
        }
        return ImmutableList.of();
    }

    private Map<Player, Float> score(List<Trick> tricks, SetupState setup, Set<Card> handfulShown) {
        PlayerState playerState = setup.getPlayerState();
        Set<String> attackerIds = Players.getIds(playerState.getTaker(), playerState.getPartner());
        Set<String> defenderIds = Players.getIds(playerState.getPlayers().stream()
                .filter(p -> !p.equals(playerState.getTaker()) && !p.equals(playerState.getPartner()))
                .collect(Collectors.toSet()));
        float handScore = scorer.computeHandScore(
                setup.getBidding().getBid().get(),
                handfulShown,
                tricks,
                attackerIds,
                defenderIds,
                setup.getAside());

        Map<Player, Float> allScores = new HashMap<>(playerState.getPlayers().size());
        for (Player player : playerState.getPlayers()) {
            boolean isTaker = player.equals(playerState.getTaker());
            boolean isPartner = player.equals(playerState.getPartner());
            allScores.put(player, computePlayerScore(handScore, isTaker, isPartner, setup.getPartnerCard().orElse(null)));
        }
        return allScores;
    }

    private float computePlayerScore(float handScore, boolean isTaker, boolean isPartner, @Nullable Card partnerCard) {
        if (isTaker) {
            if (partnerCard == null) {
                // Couldn't call partner
                return handScore * 3;
            } else if (isPartner) {
                // Called him/herself
                return handScore * 3;
            } else {
                return handScore * 2;
            }
        } else if (isPartner) {
            // Note that this must go after the 'isTaker' check since we handle the case when a taker calls
            // him/herself in that block
            return handScore;
        } else {
            // Defender
            return -handScore;
        }
    }

    @Value.Immutable
    public interface Result {
        Bidding getBidding();

        String getFirstPlayer();

        String getTaker();

        /**
         * @return The partner of the taker. If the taker could not call a partner (because their hand included all
         * rois, dames, and cavaliers) then this will be the taker
         */
        String getPartner();

        Optional<Card> getPartnerCard();

        Set<Card> getDog();

        Set<Card> getAside();

        List<Trick> getTricks();

        Map<String, Float> getScore();
    }

    static class Player implements Bidder {
        private final String id;
        private final Hand originalHand;
        private final Strategy strategy;

        private Hand currentHand;

        private Player(String id, Hand originalHand, Strategy strategy, Hand currentHand) {
            this.id = id;
            this.originalHand = originalHand;
            this.strategy = strategy;
            this.currentHand = currentHand;
        }

        public static Player create(String id,
                                    Hand hand,
                                    BiddingStrategy biddingStrategy,
                                    PartnerStrategy partnerStrategy,
                                    DogStrategy dogStrategy,
                                    TrickStrategy trickStrategy) {
            Strategy strategy = new Strategy(biddingStrategy, partnerStrategy, dogStrategy, trickStrategy);
            return new Player(id, ImmutableHand.copyOf(hand), strategy, ImmutableHand.copyOf(hand));
        }

        @Override
        public String getId() {
            return id;
        }

        Hand getOriginalHand() {
            return originalHand;
        }

        Hand getCurrentHand() {
            return currentHand;
        }

        @Override
        public Optional<Bid> bid(Bidding state) {
            return strategy.bid(getOriginalHand(), state);
        }

        public Optional<Card> callPartnerIfPossible(Bidding bidding) {
            return strategy.callIfPossible(getOriginalHand(), bidding);
        }

        public Set<Card> chooseAside(Set<Card> dog, Bidding bidding, @Nullable Card partnerCard) {
            Set<Card> aside = strategy.chooseAside(getOriginalHand(), dog, bidding, partnerCard);
            this.currentHand = ImmutableHand.of(Sets.difference(Sets.union(getOriginalHand().getCards(), dog), aside));
            return aside;
        }

        public Set<Card> checkHandful(Bidding bidding) {
            return strategy.checkHandful(getCurrentHand(), bidding);
        }

        public void handleHandful(Set<Card> shownCards, Bidding bidding, List<String> playerIds) {
            strategy.handleHandful(shownCards, bidding, playerIds, getCurrentHand());
        }

        /**
         * Only public information is passed in. So, for instance, the previous tricks of the round are not passed in.
         * Implementations should feel free to record previous tricks as is necessary
         */
        public Card play(List<Play> currentTrick,
                         Bidding bidding,
                         List<String> playerIds,
                         String takerId,
                         Card partnerCard) {
            Hand hand = getCurrentHand();
            Card play = strategy.pickCard(currentTrick, hand, bidding, playerIds, takerId, partnerCard);
            Tricks.verifyPlay(hand, play, currentTrick);
            this.currentHand = ImmutableHand.of(Sets.difference(hand.getCards(), ImmutableSet.of(play)));
            return play;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("originalHand", originalHand)
                    .add("currentHand", currentHand)
                    .add("strategy", strategy)
                    .toString();
        }

        private static class Strategy implements BiddingStrategy, PartnerStrategy, DogStrategy, TrickStrategy {
            private final BiddingStrategy biddingStrategy;
            private final PartnerStrategy partnerStrategy;
            private final DogStrategy dogStrategy;
            private final TrickStrategy trickStrategy;

            Strategy(BiddingStrategy biddingStrategy,
                     PartnerStrategy partnerStrategy,
                     DogStrategy dogStrategy,
                     TrickStrategy trickStrategy) {
                this.biddingStrategy = biddingStrategy;
                this.partnerStrategy = partnerStrategy;
                this.dogStrategy = dogStrategy;
                this.trickStrategy = trickStrategy;
            }

            @Override
            public Optional<Bid> bid(Hand hand, Bidding bidding) {
                return biddingStrategy.bid(hand, bidding);
            }

            @Override
            public Optional<Card> callIfPossible(Hand hand, Bidding bidding) {
                return partnerStrategy.callIfPossible(hand, bidding);
            }

            @Override
            public Set<Card> chooseAside(Hand hand, Set<Card> dog, Bidding bidding, @Nullable Card partnerCard) {
                return dogStrategy.chooseAside(hand, dog, bidding, partnerCard);
            }

            @Override
            public Set<Card> checkHandful(Hand hand, Bidding bidding) {
                return trickStrategy.checkHandful(hand, bidding);
            }

            @Override
            public void handleHandful(Set<Card> shownCards, Bidding bidding, List<String> playerIds, Hand currentHand) {
                trickStrategy.handleHandful(shownCards, bidding, playerIds, currentHand);
            }

            @Override
            public Card pickCard(List<Play> currentTrick,
                                 Hand hand,
                                 Bidding bidding,
                                 List<String> playerIds,
                                 String takerId,
                                 @Nullable Card partnerCard) {
                return trickStrategy.pickCard(currentTrick, hand, bidding, playerIds, takerId, partnerCard);
            }

            @Override
            public String toString() {
                return MoreObjects.toStringHelper(this)
                        .add("biddingStrategy", biddingStrategy)
                        .add("partnerStrategy", partnerStrategy)
                        .add("dogStrategy", dogStrategy)
                        .add("trickStrategy", trickStrategy)
                        .toString();
            }
        }
    }

    @Value.Immutable
    interface SetupState {
        Deal getDeal();

        PlayerState getPlayerState();

        Bidding getBidding();

        Optional<Card> getPartnerCard();

        Set<Card> getAside();

        @Value.Immutable
        interface PlayerState {
            List<Player> getPlayers();

            Player getTaker();

            Player getPartner();

            @Value.Derived
            default Map<String, Player> getPlayersById() {
                return Maps.uniqueIndex(getPlayers(), Player::getId);
            }

            default Player getPlayer(String playerId) {
                Preconditions.checkArgument(getPlayersById().containsKey(playerId), "No player found with id '%s'", playerId);
                return getPlayersById().get(playerId);
            }

            @Value.Derived
            default Map<Player, Player> getNextPlayers() {
                List<Player> players = getPlayers();
                return IntStream.range(0, players.size()).boxed().collect(Collectors.toMap(
                        players::get,
                        i -> players.get((i + 1) % players.size())));
            }

            default Player getNextPlayer(Player player) {
                Preconditions.checkArgument(getNextPlayers().containsKey(player), "No next player found for '%s'", player);
                return getNextPlayers().get(player);
            }
        }
    }
}
