/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.inventory.*;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroup;
import de.timesnake.basic.game.util.game.TablistGroupType;
import de.timesnake.basic.game.util.user.SpectatorUser;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.library.basic.util.Status;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
    this.setDefault();
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
      return this.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME) ? null : MicroGamesServer.getTablistManager().getGameTeam();
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

  public class VoteInventory implements InventoryHolder, UserInventoryInteractListener,
      UserInventoryClickListener {

    private final ExItemStack item = new ExItemStack(Material.NETHER_STAR,
        ChatColor.GOLD + "Voting");
    private final ExInventory inv;
    private final HashMap<ExItemStack, MicroGame> gamesByItem = new HashMap<>();

    public VoteInventory(Collection<MicroGame> games) {
      this.inv = new ExInventory(games.size(), Component.text("Voting"), this);

      int i = 0;
      for (MicroGame game : games) {
        ExItemStack item = new ExItemStack(i, game.getMaterial(),
            ChatColor.GOLD + game.getDisplayName(),
            List.of(ChatColor.WHITE + game.getHeadLine()));
        item.hideAll();
        this.inv.setItemStack(item);
        i++;
        this.gamesByItem.put(item, game);
      }

      Server.getInventoryEventManager().addClickListener(this, this);
      Server.getInventoryEventManager().addInteractListener(this, this.item);
    }

    @Override
    public @NotNull Inventory getInventory() {
      return this.inv.getInventory();
    }

    public ExItemStack getItem() {
      return item;
    }

    @Override
    public void onUserInventoryClick(UserInventoryClickEvent event) {
      if (!event.getUser().equals(MicroGamesUser.this)) {
        return;
      }

      ExItemStack item = event.getClickedItem();
      MicroGame microGame = this.gamesByItem.get(item);

      if (microGame != null) {
        if (MicroGamesUser.this.votedGames.contains(microGame)) {
          microGame.removeVote();
          MicroGamesUser.this.votedGames.remove(microGame);

          item.disenchant();
          item.setLore(ChatColor.WHITE + microGame.getHeadLine());
          MicroGamesUser.this.logger.info("'{}' devoted '{}'", MicroGamesUser.this.getName(), microGame.getName());
        } else {
          MicroGamesUser.this.votedGames.add(microGame);
          microGame.addVote();

          item.enchant();
          item.setLore(ChatColor.WHITE + microGame.getHeadLine(), "", "§aVoted");
          MicroGamesUser.this.logger.info("'{}' voted for '{}'", MicroGamesUser.this.getName(), microGame.getName());
        }

        this.gamesByItem.put(item, microGame);
        this.inv.setItemStack(item);
      }

      event.setCancelled(true);
    }


    @Override
    public void onUserInventoryInteract(UserInventoryInteractEvent event) {
      event.getUser().openInventory(this.inv);
    }
  }

}
