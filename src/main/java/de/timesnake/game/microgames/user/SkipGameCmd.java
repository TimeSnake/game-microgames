/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.chat.cmd.Argument;
import de.timesnake.basic.bukkit.util.chat.cmd.CommandListener;
import de.timesnake.basic.bukkit.util.chat.cmd.Completion;
import de.timesnake.basic.bukkit.util.chat.cmd.Sender;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.chat.Code;

public class SkipGameCmd implements CommandListener {

  private final Code perm = Plugin.MICRO_GAMES.createPermssionCode("microgames.skip");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd,
      Arguments<Argument> args) {
    if (!args.isLengthEquals(1, true)) {
      return;
    }

    if (args.get(0).equalsIgnoreCase("skip")) {
      if (!sender.hasPermission(this.perm)) {
        return;
      }

      if (MicroGamesServer.getCurrentGame() != null && MicroGamesServer.getCurrentGame()
          .isGameRunning()) {
        MicroGamesServer.skipGame();
      }
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(new Completion("skip"));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
