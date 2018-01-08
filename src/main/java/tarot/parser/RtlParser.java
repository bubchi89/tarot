package tarot.parser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import tarot.game.ImmutablePlay;
import tarot.game.ImmutableTrick;
import tarot.game.Trick;
import tarot.game.Trick.Play;
import tarot.state.Card;
import tarot.state.Card.Suited;
import tarot.state.Card.Trump;
import tarot.state.Hand;
import tarot.state.Hands;
import tarot.state.ImmutableHand;

/**
 * Robert's Tarot Language (rtl)
 */
public class RtlParser {
    private static final Pattern PLAYER_ID_PATTERN = Pattern.compile("(\\w+)([+*]*)");

    private static Map<String, Card> rtlCardToCard = ImmutableMap.<String, Card>builder()
            .put("cr", Suited.C_R)
            .put("cd", Suited.C_D)
            .put("cc", Suited.C_C)
            .put("cv", Suited.C_V)
            .put("c10", Suited.C_10)
            .put("c9", Suited.C_9)
            .put("c8", Suited.C_8)
            .put("c7", Suited.C_7)
            .put("c6", Suited.C_6)
            .put("c5", Suited.C_5)
            .put("c4", Suited.C_4)
            .put("c3", Suited.C_3)
            .put("c2", Suited.C_2)
            .put("c1", Suited.C_1)
            .put("dr", Suited.D_R)
            .put("dd", Suited.D_D)
            .put("dc", Suited.D_C)
            .put("dv", Suited.D_V)
            .put("d10", Suited.D_10)
            .put("d9", Suited.D_9)
            .put("d8", Suited.D_8)
            .put("d7", Suited.D_7)
            .put("d6", Suited.D_6)
            .put("d5", Suited.D_5)
            .put("d4", Suited.D_4)
            .put("d3", Suited.D_3)
            .put("d2", Suited.D_2)
            .put("d1", Suited.D_1)
            .put("hr", Suited.H_R)
            .put("hd", Suited.H_D)
            .put("hc", Suited.H_C)
            .put("hv", Suited.H_V)
            .put("h10", Suited.H_10)
            .put("h9", Suited.H_9)
            .put("h8", Suited.H_8)
            .put("h7", Suited.H_7)
            .put("h6", Suited.H_6)
            .put("h5", Suited.H_5)
            .put("h4", Suited.H_4)
            .put("h3", Suited.H_3)
            .put("h2", Suited.H_2)
            .put("h1", Suited.H_1)
            .put("sr", Suited.S_R)
            .put("sd", Suited.S_D)
            .put("sc", Suited.S_C)
            .put("sv", Suited.S_V)
            .put("s10", Suited.S_10)
            .put("s9", Suited.S_9)
            .put("s8", Suited.S_8)
            .put("s7", Suited.S_7)
            .put("s6", Suited.S_6)
            .put("s5", Suited.S_5)
            .put("s4", Suited.S_4)
            .put("s3", Suited.S_3)
            .put("s2", Suited.S_2)
            .put("s1", Suited.S_1)
            .put("t1", Trump.ONE)
            .put("t2", Trump.TWO)
            .put("t3", Trump.THREE)
            .put("t4", Trump.FOUR)
            .put("t5", Trump.FIVE)
            .put("t6", Trump.SIX)
            .put("t7", Trump.SEVEN)
            .put("t8", Trump.EIGHT)
            .put("t9", Trump.NINE)
            .put("t10", Trump.TEN)
            .put("t11", Trump.ELEVEN)
            .put("t12", Trump.TWELVE)
            .put("t13", Trump.THIRTEEN)
            .put("t14", Trump.FOURTEEN)
            .put("t15", Trump.FIFTEEN)
            .put("t16", Trump.SIXTEEN)
            .put("t17", Trump.SEVENTEEN)
            .put("t18", Trump.EIGHTEEN)
            .put("t19", Trump.NINETEEN)
            .put("t20", Trump.TWENTY)
            .put("t21", Trump.TWENTY_ONE)
            .put("f", Trump.FOOL)
            .build();

    public static void main(String[] args) {
        Path rtlPath = Paths.get(args[0]);
        Preconditions.checkArgument(rtlPath.toFile().exists(), "%s does not exist", rtlPath);

        try {
            List<String> rtlLines =
                    Files.readAllLines(rtlPath).stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
            TricksAndHands result = parse(rtlLines);
            for (Trick trick : result.getTricks()) {
                System.out.println(trick);
            }
            for (Hand hand : result.getHands()) {
                System.out.println(Hands.getSortedCards(hand));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static TricksAndHands parse(List<String> rtlLines) {
        Preconditions.checkArgument(
                rtlLines.size() == Trick.TRICKS_PER_ROUND + 1,
                "There must be %s lines (one for each trick and one for the header)",
                Trick.TRICKS_PER_ROUND + 1);

        PlayerInfo playerInfo = parsePlayerIds(rtlLines.get(0));
        List<Trick> tricks = parseTricks(rtlLines.subList(1, rtlLines.size()), playerInfo);
        List<Hand> hands = parseHands(tricks, playerInfo);

        return ImmutableTricksAndHands.builder().addAllTricks(tricks).addAllHands(hands).build();
    }

    private static PlayerInfo parsePlayerIds(String rtlPlayerIds) {
        ImmutablePlayerInfo.Builder builder = ImmutablePlayerInfo.builder();
        for (String term : Splitter.on(Pattern.compile("\\s")).split(rtlPlayerIds)) {
            Matcher matcher = PLAYER_ID_PATTERN.matcher(term);
            if (!matcher.matches()) {
                throw new ParseException("Could not parse player id: " + term);
            } else {
                String playerId = matcher.group(1);
                builder.addPlayerIds(playerId);
                if (matcher.groupCount() > 1) {
                    if (matcher.group(2).contains("*")) {
                        builder.taker(playerId);
                    }
                    if (matcher.group(2).contains("+")) {
                        builder.partner(playerId);
                    }
                }
            }
        }
        PlayerInfo playerInfo = builder.build();
        if (playerInfo.getPlayerIds().stream().distinct().count() != playerInfo.getPlayerIds().size()) {
            throw new ParseException("All player ids must be unique");
        }
        return playerInfo;
    }

    private static List<Trick> parseTricks(List<String> rtlTricks, PlayerInfo playerInfo) {
        List<Trick> tricks = new ArrayList<>(Trick.TRICKS_PER_ROUND);
        for (int numTrick = 0; numTrick < rtlTricks.size() - 1; numTrick++) {
            tricks.add(parseTrick(rtlTricks.get(numTrick), numTrick + 1, playerInfo, ImmutableSet.of()));
        }
        tricks.add(parseLastTrick(Iterables.getLast(rtlTricks), playerInfo, tricks));
        return tricks;
    }

    private static Trick parseLastTrick(String rtlLastTrick, PlayerInfo playerInfo, List<Trick> tricks) {
        if (tricks.stream().allMatch(t -> playerInfo.getDefenders().contains(t.getWinner().getPlayer()))) {
            return parseTrick(rtlLastTrick, Trick.TRICKS_PER_ROUND, playerInfo, playerInfo.getDefenders());
        } else if (tricks.stream().noneMatch(t -> playerInfo.getDefenders().contains(t.getWinner().getPlayer()))) {
            return parseTrick(rtlLastTrick, Trick.TRICKS_PER_ROUND, playerInfo, playerInfo.getAttackers());
        } else {
            return parseTrick(rtlLastTrick, Trick.TRICKS_PER_ROUND, playerInfo, ImmutableSet.of());
        }
    }

    private static Trick parseTrick(String rtlTrick,
                                    int numTrick,
                                    PlayerInfo playerInfo,
                                    Set<String> playersWithStrongFool) {
        int numToRotate = -1;
        int numTerm = 0;
        List<Play> plays = new ArrayList<>();
        for (String term : Splitter.on(Pattern.compile("\\s")).split(rtlTrick)) {
            if (term.endsWith("*")) {
                term = term.substring(0, term.length() - 1);
                numToRotate = numTerm;
            }
            plays.add(ImmutablePlay.of(playerInfo.getPlayerIds().get(numTerm), parseCard(term)));

            numTerm++;
        }
        if (numToRotate < 0) {
            throw new ParseException(
                    "Could not parse trick. Exactly one card in each trick should be marked as the first play with an " +
                            "asterisk (e.g. 'c1*'): " + rtlTrick);
        }
        // Rotate backwards instead of forwards
        Collections.rotate(plays, -numToRotate);
        return ImmutableTrick.builder()
                .numberInRound(numTrick)
                .addAllPlays(plays)
                .playersWithStrongFool(playersWithStrongFool)
                .build();
    }

    private static Card parseCard(String rtlCard) {
        if (!rtlCardToCard.containsKey(rtlCard)) {
            throw new ParseException("Unknown card: " + rtlCard);
        } else {
            return rtlCardToCard.get(rtlCard);
        }
    }

    private static List<Hand> parseHands(List<Trick> tricks, PlayerInfo playerInfo) {
        Map<String, ImmutableHand.Builder> hands = Maps.toMap(playerInfo.getPlayerIds(), p -> ImmutableHand.builder());
        for (Trick trick : tricks) {
            for (Play play : trick.getPlays()) {
                hands.get(play.getPlayer()).addCards(play.getCard());
            }
        }
        return hands.values().stream().map(b -> b.build()).collect(Collectors.toList());
    }

    @Value.Immutable
    interface TricksAndHands {
        List<Trick> getTricks();

        List<Hand> getHands();

        // TODO: (rliu) add a check that a player did not play improperly? It seems complicated to enforce that in a
        // data-type but it's also weird to permit constructing such an invalid instance
    }

    @Value.Immutable
    interface PlayerInfo {
        List<String> getPlayerIds();

        String getTaker();

        String getPartner();

        @Value.Lazy
        default Set<String> getAttackers() {
            return ImmutableSet.of(getTaker(), getPartner());
        }

        @Value.Lazy
        default Set<String> getDefenders() {
            return getPlayerIds().stream()
                    .filter(p -> !p.equals(getTaker()) && !p.equals(getPartner()))
                    .collect(Collectors.toSet());
        }
    }
}
