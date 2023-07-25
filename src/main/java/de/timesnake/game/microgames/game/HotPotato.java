/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDropItemEvent;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.player.UserSet;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class HotPotato extends ScoreGame<Integer> implements Listener {

  protected static final ExItemStack HOT_POTATO = new ExItemStack(Material.POTATO)
      .setDisplayName("§cHot Potato");
  private static final int TIME = 180;
  private static final int COOLDOWN_TICKS = 40;
  private static final int SLOW_TICKS = 40;
  private static final int SLOW_AMPLIFIER = 2;
  private static final int GLOW_TICKS = 7 * 20;

  private final Set<User> holders = new UserSet<>();
  private final Set<User> cooldownUsers = new UserSet<>();

  private Integer time = TIME;
  private BukkitTask task;


  public HotPotato() {
    super("hotpotato", "Hot Potato", Material.BAKED_POTATO,
        "Don't hold the potato", 2, null);

    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public void load() {
    super.load();

    super.sideboard.setScore(5, "§9§lTime left");
    super.sideboard.setScore(4, Chat.getTimeString(this.time));
    super.sideboard.setScore(3, "§f---------------");
  }

  @Override
  protected void loadDelayed() {
    super.loadDelayed();

    for (User user : Server.getPreGameUsers()) {
      user.lockInventoryItemMove();
      user.lockInventory();
    }
  }

  private void giveHotPotatoTo(User user) {
    this.holders.add(user);
    user.getInventory().setHelmet(new ExItemStack(Material.GOLDEN_HELMET));
    user.getInventory().setChestplate(new ExItemStack(Material.GOLDEN_CHESTPLATE));
    user.getInventory().setLeggings(new ExItemStack(Material.GOLDEN_LEGGINGS));
    user.getInventory().setBoots(new ExItemStack(Material.GOLDEN_BOOTS));
    user.fillHotBar(HOT_POTATO);
    user.playNote(Instrument.PIANO, Note.natural(0, Note.Tone.C));

    if (this.time.equals(TIME)) {
      user.sendPluginTDMessage(Plugin.MICRO_GAMES, "§sYou have the §chot potato");
    }

    user.addPotionEffect(PotionEffectType.SLOW, SLOW_TICKS, SLOW_AMPLIFIER);
    this.cooldownUsers.add(user);
    Server.runTaskLaterSynchrony(() -> this.cooldownUsers.remove(user), COOLDOWN_TICKS, GameMicroGames.getPlugin());
  }

  private void removeHotPotatoFrom(User user) {
    this.holders.remove(user);
    user.playNote(Instrument.PIANO, Note.natural(1, Note.Tone.C));
    user.clearInventory();
  }

  private void chooseHolder() {
    Server.getInGameUsers().stream()
        .filter(u -> !this.holders.contains(u))
        .max(Comparator.comparingDouble(u -> u.getLocation().distanceSquared(this.getSpawnLocation())))
        .ifPresent(this::giveHotPotatoTo);
  }

  @Override
  public void start() {
    super.start();

    for (int i = 0; i < Server.getInGameUsers().size() / 2; i++) {
      this.chooseHolder();
    }

    this.task = Server.runTaskTimerSynchrony(() -> {
      this.time--;
      super.sideboard.setScore(4, Chat.getTimeString(time));

      for (User user : this.holders) {
        this.updateUserScore(user, (u, v) -> v + 1);
      }

      if (time % 20 == 0) {
        MicroGamesServer.broadcastMicroGamesTDMessage("§pThe best Player is now §cglowing!");
        MicroGamesServer.broadcastNote(Instrument.PIANO, Note.natural(0, Note.Tone.C));

        List<Map.Entry<MicroGamesUser, Integer>> entries = new ArrayList<>(this.scores.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getValue));
        entries.get(0).getKey().addPotionEffect(PotionEffectType.GLOWING, GLOW_TICKS, 1);
      }

      if (this.time == 0) {
        this.stop();
      }
    }, 0, 20, GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {
    super.calcPlaces(false);

    if (this.task != null) {
      this.task.cancel();
    }

    super.stop();
  }

  @Override
  public void reset() {
    super.reset();
    this.time = TIME;
    this.holders.clear();
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
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

    if (this.cooldownUsers.contains(damager)) {
      e.setCancelled(true);
      return;
    }

    if (!this.holders.contains(damager)) {
      e.setCancelled(true);
      return;
    }

    if (this.holders.contains(target)) {
      e.setCancelled(true);
      return;
    }

    this.removeHotPotatoFrom(damager);
    this.giveHotPotatoTo(target);
  }
}


