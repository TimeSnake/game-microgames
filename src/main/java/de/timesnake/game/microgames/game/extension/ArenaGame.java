/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.extension;

import de.timesnake.basic.bukkit.util.world.BlockPolygon;
import de.timesnake.game.microgames.game.basis.MicroGameExtensionBase;

public interface ArenaGame extends MicroGameExtensionBase {

  int ARENA_POLYGON_START_INDEX = 100;
  int ARENA_POLYGON_END_INDEX = 200;

  default BlockPolygon getArena() {
    return this.getCurrentMap().getBlockPolygon(ARENA_POLYGON_START_INDEX, ARENA_POLYGON_END_INDEX);
  }
}
