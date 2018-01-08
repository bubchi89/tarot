package tarot.game;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import tarot.game.Round.Player;

public class Players {
    private Players() {
        // Prevent instantiation
    }

    public static Set<String> getIds(Player... players) {
        return getIds(ImmutableSet.copyOf(players));
    }

    public static Set<String> getIds(Set<Player> players) {
        return players.stream().map(p -> p.getId()).collect(Collectors.toSet());
    }

    public static List<String> getIds(List<Player> players) {
        return players.stream().map(p -> p.getId()).collect(Collectors.toList());
    }
}
