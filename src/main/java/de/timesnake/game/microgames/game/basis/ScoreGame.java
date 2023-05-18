/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.game.microgames.user.MicroGamesUser;
import java.util.HashMap;
import java.util.Map.Entry;
import org.bukkit.Material;

public abstract class ScoreGame<Score extends Comparable<Score>> extends MicroGame {

  protected HashMap<MicroGamesUser, Score> scores = new HashMap<>();

  public ScoreGame(String name, String displayName, Material material, String description,
      Integer minPlayers, int maxTimeSec) {
    super(name, displayName, material, description, minPlayers, maxTimeSec);
  }

  @Override
  public void load() {
    super.load();

    for (User user : Server.getPreGameUsers()) {
      this.scores.put(((MicroGamesUser) user), this.getDefaultScore());
    }
  }

  protected void calcPlaces(boolean highest) {
    for (MicroGamesUser user : scores.entrySet().stream()
        .sorted(Entry.comparingByValue()).map(Entry::getKey).toList()) {
      if (highest) {
        this.placement.addFirst(user);
      } else {
        this.placement.addLast(user);
      }
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
}
