/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.inventory.ExInventory;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroup;
import de.timesnake.basic.game.util.game.TablistGroupType;
import de.timesnake.basic.game.util.user.SpectatorUser;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.library.basic.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MicroGamesUser extends SpectatorUser {

  private final Logger logger = LogManager.getLogger("micro-games.user");

  private final Set<MicroGame> votedGames = new HashSet<>();

  private final VoteInventory voteInventory;

  private Integer points = 0;
  private Integer place;

  public MicroGamesUser(Player player) {
    super(player);
    this.voteInventory = new VoteInventory(MicroGamesServer.getGames());
  }

  @Override
  public void setSpectatorInventory() {
    super.setSpectatorInventory();
    this.setItem(6, this.voteInventory.getItem());
    this.setItem(7, PartyManager.ITEM);
  }

  public void joinGame() {
    this.setStatus(Status.User.PRE_GAME);
    MicroGamesServer.getTablistManager().getTablist().reloadEntry(this, true);
    this.resetPlayerProperties();
    this.unlockAll();
    this.clearInventory();
    this.setCollitionWithEntites(true);
    for (User user : Server.getUsers()) {
      user.showUser(this);
    }
  }

  public void clearVotes() {
    for (MicroGame microGame : this.votedGames) {
      microGame.removeVote();
    }
    this.votedGames.clear();
  }

  @Override
  public TablistGroup getTablistGroup(de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroupType type) {
    if (type.equals(TablistGroupType.GAME_TEAM)) {
      return this.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME) ? null :
          MicroGamesServer.getTablistManager().getGameTeam();
    }
    return super.getTablistGroup(type);
  }

  public Integer getPoints() {
    return points;
  }

  public void setPoints(Integer points) {
    this.points = points;
  }

  public void resetPoints() {
    this.points = 0;
  }

  public void addPoints(Integer points) {
    this.points += points;
  }

  public Integer getPlace() {
    return place;
  }

  public void setPlace(Integer place) {
    this.place = place;
  }

  public void resetPlace() {
    this.place = null;
  }

  public boolean hasPlace() {
    return this.place != null;
  }

  public class VoteInventory {

    private final ExItemStack item;
    private final ExInventory inv;

    public VoteInventory(Collection<MicroGame> games) {
      this.inv = new ExInventory(games.size(), "Voting");

      this.item = new ExItemStack(Material.NETHER_STAR)
          .setDisplayName("§hVoting")
          .onInteract(e -> e.getUser().openInventory(this.inv));

      int i = 0;
      for (MicroGame game : games) {
        ExItemStack item = new ExItemStack(i, game.getMaterial())
            .setDisplayName("§h" + game.getDisplayName())
            .setLore("§f" + game.getHeadLine())
            .hideAll();
        item.onClick(e -> {
          if (MicroGamesUser.this.votedGames.contains(game)) {
            game.removeVote();
            MicroGamesUser.this.votedGames.remove(game);

            item.disenchant();
            item.setLore(ChatColor.WHITE + game.getHeadLine());
            MicroGamesUser.this.logger.info("'{}' devoted '{}'", MicroGamesUser.this.getName(), game.getName());
          } else {
            MicroGamesUser.this.votedGames.add(game);
            game.addVote();

            item.enchant();
            item.setLore(ChatColor.WHITE + game.getHeadLine(), "", "§aVoted");
            MicroGamesUser.this.logger.info("'{}' voted for '{}'", MicroGamesUser.this.getName(), game.getName());
          }
          this.inv.setItemStack(item);
        }, true);

        this.inv.setItemStack(item);
        i++;
      }
    }

    public ExItemStack getItem() {
      return item;
    }
  }

}
