/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.extension.ArenaGame;
import org.bukkit.Material;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * @param <Score>
 * @deprecated in favour of {@link ArenaGame}
 */
@Deprecated
public abstract class BoxedScoreGame<Score extends Comparable<Score>> extends ScoreGame<Score> {

  protected static final Integer FIRST_CORNER_INDEX = 3;
  protected static final Integer SECOND_CORNER_INDEX = 4;

  public BoxedScoreGame(String name, String displayName, Material material, String headLine, List<String> description,
                        Integer minPlayers, Duration maxTime) {
    super(name, displayName, material, headLine, description, minPlayers, maxTime);
  }

  public ExLocation getFirstCorner() {
    return this.currentMap.getLocation(FIRST_CORNER_INDEX);
  }

  public ExLocation getSecondCorner() {
    return this.currentMap.getLocation(SECOND_CORNER_INDEX);
  }

  public Collection<ExBlock> getBlocksWithinBox() {
    return this.currentMap.getWorld().getBlocksWithinCubic(this.getFirstCorner(), this.getSecondCorner());
  }
}
