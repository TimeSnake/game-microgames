/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.world.ExLocation;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

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

  public Collection<Block> getBlocksWithinBox() {
    return this.currentMap.getWorld().getBlocksWithinCubic(this.getFirstCorner(), this.getSecondCorner());
  }
}
