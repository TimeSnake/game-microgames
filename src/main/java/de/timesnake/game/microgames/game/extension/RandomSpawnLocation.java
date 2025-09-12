/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.extension;

import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.MicroGameExtensionBase;

public interface RandomSpawnLocation extends MicroGameExtensionBase {

  int START_LOCATION_START_INDEX = 20;
  int START_LOCATION_END_INDEX = 30;

  @Override
  default ExLocation getSpawnLocation() {
    ExLocation location = this.getCurrentMap().getLocation(this.getRandom().nextInt(START_LOCATION_START_INDEX,
        START_LOCATION_END_INDEX));
    return location != null ? location : this.getSpawnLocation();
  }
}
