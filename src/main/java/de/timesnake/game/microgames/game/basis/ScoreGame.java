/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class ScoreGame<Score extends Comparable<Score>> extends MicroGame {

  protected HashMap<MicroGamesUser, Score> scores = new HashMap<>();

  public ScoreGame(String name, String displayName, Material material, String headLine, List<String> description,
                   Integer minPlayers, Duration maxTime) {
    super(name, displayName, material, headLine, description, minPlayers, maxTime);
  }

  @Override
  public void load() {
    super.load();

    for (User user : Server.getPreGameUsers()) {
      this.scores.put(((MicroGamesUser) user), this.getDefaultScore());
    }

    if (this.hasSideboard()) {
      super.sideboard.setScore(1, "§c§l" + this.getScoreName());
      super.sideboard.setScore(0, "§f" + this.getDefaultScore());
    }
  }

  protected void calcPlaces(boolean highestWins) {
    LinkedList<MicroGamesUser> users = scores.entrySet().stream().sorted(Entry.comparingByValue())
        .map(Entry::getKey).collect(Collectors.toCollection(LinkedList::new));

    int place = 1;
    int offset = 0;
    MicroGamesUser previous = null;

    Iterator<MicroGamesUser> it = highestWins ? users.descendingIterator() : users.iterator();
    while (it.hasNext()) {
      MicroGamesUser user = it.next();
      if (previous != null && this.scores.get(previous).equals(this.scores.get(user))) {
        user.setPlace(place - (place > 1 ? -1 : 0));
        offset++;
      } else {
        place += offset;
        user.setPlace(place);
        offset = 1;
      }

      previous = user;
    }
  }

  @Override
  protected String getWinMessage(MicroGamesUser user, int place) {
    return super.getWinMessage(user, place) + "  §7(" + this.scores.get(user) + ")";
  }

  @Override
  public void reset() {
    super.reset();
    this.scores.clear();
  }

  public abstract Score getDefaultScore();

  public String getScoreName() {
    return "Score";
  }

  public void updateUserScore(User user, BiFunction<MicroGamesUser, Score, Score> updateFunction) {
    Score score = this.scores.compute(((MicroGamesUser) user), updateFunction);
    if (this.hasSideboard()) {
      user.setSideboardScore(0, "§f" + score);
    }
  }
}
