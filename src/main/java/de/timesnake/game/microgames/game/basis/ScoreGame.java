/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    List<MicroGamesUser> users = scores.entrySet().stream().sorted(Entry.comparingByValue())
        .map(Entry::getKey).collect(Collectors.toCollection(ArrayList::new));

    if (highestWins) {
      Collections.reverse(users);
    }

    int place = 1;
    MicroGamesUser previous = null;

    for (MicroGamesUser user : users) {
      if (previous != null && this.scores.get(previous).equals(this.scores.get(user))) {
        user.setPlace(previous.getPlace());
      } else {
        user.setPlace(place);
      }

      previous = user;
      place++;
    }
  }

  @Override
  public String getWinMessage(MicroGamesUser user, int place) {
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
    if (!this.isGameRunning()) {
      return;
    }

    this.scores.compute(((MicroGamesUser) user), updateFunction);
    if (this.hasSideboard()) {
      this.updateUserScoreOnSideboard((MicroGamesUser) user);
    }
  }

  public void updateUserScoreOnSideboard(MicroGamesUser user) {
    user.setSideboardScore(0, "§f" + this.scores.getOrDefault(user, this.getDefaultScore()));
  }
}
