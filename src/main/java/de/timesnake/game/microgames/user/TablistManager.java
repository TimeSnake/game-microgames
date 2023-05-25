/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.group.DisplayGroup;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.scoreboard.ScoreboardManager;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroupType;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistableGroup;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistableRemainTeam;
import de.timesnake.basic.bukkit.util.user.scoreboard.TeamTablist;
import de.timesnake.basic.bukkit.util.user.scoreboard.TeamTablistBuilder;
import de.timesnake.library.basic.util.Status;
import java.util.List;

public class TablistManager {

  private final TeamTablist tablist;
  private final TablistableGroup gameTeam;
  private final TablistableRemainTeam spectatorTeam;

  public TablistManager() {

    // tablist

    this.gameTeam = new TablistableGroup() {
      @Override
      public String getTablistRank() {
        return "0";
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
      public org.bukkit.ChatColor getTablistPrefixChatColor() {
        return ChatColor.WHITE;
      }

      @Override
      public org.bukkit.ChatColor getTablistChatColor() {
        return ChatColor.WHITE;
      }
    };

    this.spectatorTeam = new TablistableRemainTeam() {
      @Override
      public String getTablistName() {
        return "spec";
      }

      @Override
      public String getTablistPrefix() {
        return "";
      }

      @Override
      public org.bukkit.ChatColor getTablistPrefixChatColor() {
        return ChatColor.WHITE;
      }

      @Override
      public org.bukkit.ChatColor getTablistChatColor() {
        return ChatColor.GRAY;
      }
    };

    this.tablist = Server.getScoreboardManager()
        .registerTeamTablist(new TeamTablistBuilder("lounge_side")
            .colorType(TeamTablist.ColorType.WHITE)
            .teams(List.of(this.gameTeam))
            .teamType(TablistGroupType.DUMMY)
            .groupTypes(DisplayGroup.MAIN_TABLIST_GROUPS)
            .remainTeam(this.spectatorTeam)
            .userJoin((e, tablist) -> {
              User user = e.getUser();
              Status.User status = user.getStatus();

              if (status.equals(Status.User.OUT_GAME) || status.equals(
                  Status.User.SPECTATOR)) {
                ((TeamTablist) tablist).addRemainEntry(e.getUser());
              } else {
                tablist.addEntry(e.getUser());
              }
            })
            .userQuit((e, tablist) -> tablist.removeEntry(e.getUser())));

    this.tablist.setHeader("§6MicroGames");

    this.tablist.setFooter(ScoreboardManager.getDefaultFooter());

    Server.getScoreboardManager().setActiveTablist(this.tablist);
  }

  public TeamTablist getTablist() {
    return tablist;
  }

  public TablistableGroup getGameTeam() {
    return gameTeam;
  }

  public TablistableRemainTeam getSpectatorTeam() {
    return spectatorTeam;
  }
}