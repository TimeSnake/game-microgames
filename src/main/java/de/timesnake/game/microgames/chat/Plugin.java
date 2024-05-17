/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.chat;

public class Plugin extends de.timesnake.basic.bukkit.util.chat.Plugin {

  public static final Plugin MICRO_GAMES = new Plugin("MicroGames", "GMG");

  protected Plugin(String name, String code) {
    super(name, code);
  }
}
