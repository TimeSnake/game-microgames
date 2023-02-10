/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.user.inventory.ExInventory;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.inventory.UserInventoryClickEvent;
import de.timesnake.basic.bukkit.util.user.inventory.UserInventoryClickListener;
import de.timesnake.basic.bukkit.util.user.inventory.UserInventoryInteractEvent;
import de.timesnake.basic.bukkit.util.user.inventory.UserInventoryInteractListener;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistableGroup;
import de.timesnake.basic.game.util.game.TablistGroupType;
import de.timesnake.basic.game.util.user.SpectatorUser;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.game.MicroGame;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.library.basic.util.Status;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MicroGamesUser extends SpectatorUser {

    private final Set<MicroGame> votedGames = new HashSet<>();

    private final VoteInventory voteInventory;

    private Integer points = 0;

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
        MicroGamesServer.getTablistManager().getTablist().addEntry(this);
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
    public TablistableGroup getTablistGroup(
            de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroupType type) {
        if (type.equals(TablistGroupType.DUMMY)) {
            return MicroGamesServer.getTablistManager().getGameTeam();
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
                        List.of(ChatColor.WHITE + game.getDescription()));
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
                    item.setLore(ChatColor.WHITE + microGame.getDescription());

                    Server.printText(Plugin.MICRO_GAMES,
                            MicroGamesUser.this.getName() + " devoted " + microGame.getName(),
                            "Voting");
                } else {
                    MicroGamesUser.this.votedGames.add(microGame);
                    microGame.addVote();

                    item.enchant();
                    item.setLore(ChatColor.WHITE + microGame.getDescription(), "", "§aVoted");

                    Server.printText(Plugin.MICRO_GAMES,
                            MicroGamesUser.this.getName() + " voted for " + microGame.getName(),
                            "Voting");
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
