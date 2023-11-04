/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import org.bukkit.Material;

import java.util.List;

public class ColorPunch extends ColorSwap {

  public ColorPunch() {
    super("colorpunch",
        "ColorPunch",
        Material.RED_WOOL,
        "Try to stand on the color, which is shown in your hotbar",
        List.of("§hGoal: §plast man standing", "Stand on the color, which is shown in your hotbar.", "Punch other " +
            "players from their platforms."),
        2,
        null);
  }

  @Override
  public void prepare() {
    super.prepare();

    this.currentMap.getWorld().setPVP(true);
  }
}
