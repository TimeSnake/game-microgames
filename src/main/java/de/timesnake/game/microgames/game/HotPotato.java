/*
 * game-microgames.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.ExItemStack;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDropItemEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class HotPotato extends MicroGame implements Listener {

    protected static final ExItemStack HOT_POTATO = new ExItemStack(Material.POTATO, "§cHot Potato");
    private static final Integer START_LOCATION_INDEX = 0;
    private static final Integer SPEC_LOCATION_INDEX = 1;
    private static final Integer SPAWN_LOCATION_INDEX = 2;
    private static final Integer TIME = 180;
    private final Set<User> holders = new HashSet<>();
    private final HashMap<MicroGamesUser, Integer> potatoTimesByUser = new HashMap<>();
    private Integer time = TIME;
    private BukkitTask task;


    public HotPotato() {
        super("hotpotato", "Hot Potato", Material.BAKED_POTATO, "Don't hold the potato", 2);

        Server.registerListener(this, GameMicroGames.getPlugin());
    }

    @Override
    public Integer getLocationAmount() {
        return 3;
    }

    @Override
    public void load() {
        super.load();

        super.sideboard.setScore(6, "§9§lTime left");
        super.sideboard.setScore(5, this.getTimeString(this.time));
        super.sideboard.setScore(4, "§f---------------");
        super.sideboard.setScore(3, "§c§lPotato Time");
        super.sideboard.setScore(2, "0s");

    }

    @Override
    protected void loadDelayed() {
        for (User user : Server.getPreGameUsers()) {
            user.teleport(this.currentMap.getLocation(SPAWN_LOCATION_INDEX));
            user.lockInventoryItemMove();
            user.lockInventory();
        }
    }

    private String getTimeString(Integer time) {
        if (time >= 60) {
            return time / 60 + "min " + time % 60 + "s";
        } else {
            return time % 60 + "s";
        }
    }

    private void giveHotPotatoTo(User user) {
        if (this.holders.contains(user)) {
            user.getInventory().setHelmet(new ExItemStack(Material.GOLDEN_HELMET));
            user.getInventory().setChestplate(new ExItemStack(Material.GOLDEN_CHESTPLATE));
            user.getInventory().setLeggings(new ExItemStack(Material.GOLDEN_LEGGINGS));
            user.getInventory().setBoots(new ExItemStack(Material.GOLDEN_BOOTS));
            user.fillHotBar(HOT_POTATO);
            user.playNote(Instrument.PIANO, Note.natural(0, Note.Tone.C));
            if (this.time.equals(TIME)) {
                user.sendPluginMessage(Plugin.MICRO_GAMES, Component.text("You have the", ExTextColor.PERSONAL)
                        .append(Component.text("hot potato", ExTextColor.WARNING)));
            }
        } else {
            user.playNote(Instrument.PIANO, Note.natural(1, Note.Tone.C));
            user.clearInventory();
        }
    }

    private void chooseHolder() {
        List<User> users = new ArrayList<>(Server.getInGameUsers());
        while (true) {
            User random = users.get(this.random.nextInt(users.size()));
            if (!this.holders.contains(random)) {
                this.holders.add(random);
                this.giveHotPotatoTo(random);
                return;
            }
        }
    }

    @Override
    public void start() {
        super.start();

        for (int i = 0; i < Server.getInGameUsers().size() / 2; i++) {
            this.chooseHolder();
        }

        for (User user : MicroGamesServer.getInGameUsers()) {
            this.potatoTimesByUser.put(((MicroGamesUser) user), 0);
        }

        this.task = Server.runTaskTimerSynchrony(() -> {
            this.time--;
            super.sideboard.setScore(5, this.getTimeString(time));

            for (User user : Server.getInGameUsers()) {
                if (this.holders.contains(user)) {
                    Integer potatoTime = this.potatoTimesByUser.get(((MicroGamesUser) user));
                    if (potatoTime != null) {
                        potatoTime++;
                        this.potatoTimesByUser.put(((MicroGamesUser) user), potatoTime);
                        user.setSideboardScore(2, getTimeString(potatoTime));
                    }
                }
            }

            if (this.time <= TIME / 2) {
                if (this.time == TIME / 2) {
                    MicroGamesServer.broadcastMicroGamesMessage(Component.text("The best Player is now §cglowing!", ExTextColor.GOLD));
                    Server.broadcastTitle(Component.text("Halftime", ExTextColor.WARNING),
                            Component.text("Glowing activated!"), Duration.ofSeconds(3));
                }

                List<Map.Entry<MicroGamesUser, Integer>> entries = new ArrayList<>(this.potatoTimesByUser.entrySet());
                entries.sort(Comparator.comparingInt(Map.Entry::getValue));
                entries.get(0).getKey().addPotionEffect(PotionEffectType.GLOWING, 22, 1);
            }

            if (this.time == 0) {
                this.stop();
            }
        }, 0, 20, GameMicroGames.getPlugin());
    }

    @Override
    public void stop() {
        super.calcPlaces(this.potatoTimesByUser::get, false);

        if (this.task != null) {
            this.task.cancel();
        }

        super.stop();
    }

    @Override
    public void reset() {
        super.reset();
        this.time = TIME;
        this.potatoTimesByUser.clear();
        this.holders.clear();
    }

    @Override
    public boolean hasSideboard() {
        return true;
    }

    @Override
    public boolean onUserJoin(MicroGamesUser user) {
        return false;
    }

    @Override
    public void onUserQuit(MicroGamesUser user) {
        if (Server.getInGameUsers().size() <= 1) {
            this.stop();
        } else if (this.holders.remove(user)) {
            if (this.holders.size() == 0) {
                this.chooseHolder();
            }
        }

        if (this.holders.size() == Server.getInGameUsers().size()) {
            this.stop();
        }

    }

    @Override
    public ExLocation getSpecLocation() {
        return super.currentMap.getLocation(SPEC_LOCATION_INDEX);
    }

    @Override
    public ExLocation getStartLocation() {
        return super.currentMap.getLocation(START_LOCATION_INDEX);
    }

    public ExLocation getSpawnLocation() {
        return super.currentMap.getLocation(SPAWN_LOCATION_INDEX);
    }

    @EventHandler
    public void onUserDamage(UserDamageEvent e) {
        if (!this.isGameRunning()) {
            return;
        }
        e.setCancelDamage(true);
    }

    @EventHandler
    public void onItemDrop(UserDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onUserDamageByUser(UserDamageByUserEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        if (!e.getUserDamager().getStatus().equals(Status.User.IN_GAME)) {
            e.setCancelDamage(true);
            e.setCancelled(true);
        }

        User target = e.getUser();
        User damager = e.getUserDamager();

        if (!this.holders.contains(damager)) {
            e.setCancelled(true);
            return;
        }

        if (this.holders.contains(target)) {
            e.setCancelled(true);
            return;
        }

        this.holders.add(target);
        this.giveHotPotatoTo(target);

        this.holders.remove(damager);
        this.giveHotPotatoTo(damager);
    }
}


