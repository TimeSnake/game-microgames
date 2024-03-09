/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.core.user.scoreboard.tablist.Tablist2;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.group.DisplayGroup;
import de.timesnake.basic.bukkit.util.user.scoreboard.ScoreboardManager;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroup;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroupType;
import de.timesnake.library.chat.ExTextColor;

import java.util.ArrayList;
import java.util.List;

public class TablistManager {

  private final Tablist2 tablist;
  private final TablistGroup gameTeam;
  private final TablistGroup spectatorGroup;

  public TablistManager() {
    this.gameTeam = new TablistGroup() {
      @Override
      public int getTablistRank() {
        return 0;
      }

      @Override
      public String getTablistName() {
        return "game";
      }

      @Override
      public String getTablistPrefix() {
        return "";
      }

      @Override
      public ExTextColor getTablistPrefixChatColor() {
        return ExTextColor.WHITE;
      }

      @Override
      public ExTextColor getTablistChatColor() {
        return ExTextColor.WHITE;
      }
    };

    this.spectatorGroup = new TablistGroup() {
      @Override
      public int getTablistRank() {
        return 80;
      }

      @Override
      public String getTablistName() {
        return "spec";
      }

      @Override
      public String getTablistPrefix() {
        return "spec";
      }

      @Override
      public ExTextColor getTablistPrefixChatColor() {
        return ExTextColor.WHITE;
      }

      @Override
      public ExTextColor getTablistChatColor() {
        return ExTextColor.GRAY;
      }
    };

    List<TablistGroupType> types = new ArrayList<>();
    types.add(de.timesnake.basic.game.util.game.TablistGroupType.GAME_TEAM);
    types.addAll(DisplayGroup.MAIN_TABLIST_GROUPS);

    this.tablist = Server.getScoreboardManager()
        .registerTablist(new Tablist2.Builder("micro_games")
            .groupTypes(types)
            .colorGroupType(de.timesnake.basic.game.util.game.TablistGroupType.GAME_TEAM)
            .addDefaultGroup(de.timesnake.basic.game.util.game.TablistGroupType.GAME_TEAM, this.spectatorGroup)
            .setGroupGap(null, 2));

    this.tablist.setHeader("§6MicroGames");
    this.tablist.setFooter(ScoreboardManager.getDefaultFooter());

    Server.getScoreboardManager().setActiveTablist(this.tablist);
  }

  public Tablist2 getTablist() {
    return tablist;
  }

  public TablistGroup getGameTeam() {
    return gameTeam;
  }
}