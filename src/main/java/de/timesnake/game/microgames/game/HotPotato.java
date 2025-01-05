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
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.UserSet;
import de.timesnake.library.chat.Chat;
import org.bukkit.Color;
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
  private static final int SLOWNESS_TICKS = 40;
  private static final int SLOWNESS_AMPLIFIER = 2;
  private static final int GLOW_TICKS = 7 * 20;

  private final Set<User> holders = new UserSet<>();
  private final Set<User> cooldownUsers = new UserSet<>();

  private Integer time = TIME;
  private BukkitTask task;


  public HotPotato() {
    super("hotpotato",
        "Hot Potato",
        Material.BAKED_POTATO,
        "Don't hold the potato",
        List.of("§hGoal: §phold the potato the least time",
            "At start, be in center to not get the potato.",
            "Run away from potato holding players (in gold armor).",
            "Punch players to give them the potato.",
            "Potato holders have a cooldown, before they can give the potato away."),
        2,
        null);

    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public void load() {
    super.load();

    super.sideboard.setScore(5, "§9§lTime");
    super.sideboard.setScore(4, Chat.getTimeString(this.time));
    super.sideboard.setScore(3, Sideboard.SPACER);
  }

  @Override
  protected void applyBeforeStart() {
    super.applyBeforeStart();

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

    user.addPotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER);
  }

  private void giveCooldown(User user) {
    this.cooldownUsers.add(user);
    user.getInventory().setHelmet(ExItemStack.getLeatherArmor(Material.LEATHER_HELMET, Color.BLACK));
    user.getInventory().setChestplate(ExItemStack.getLeatherArmor(Material.LEATHER_CHESTPLATE, Color.BLACK));
    user.getInventory().setLeggings(ExItemStack.getLeatherArmor(Material.LEATHER_LEGGINGS, Color.BLACK));
    user.getInventory().setBoots(ExItemStack.getLeatherArmor(Material.LEATHER_BOOTS, Color.BLACK));

    Server.runTaskLaterSynchrony(() -> {
      this.removeCooldown(user);
      this.cooldownUsers.remove(user);
    }, COOLDOWN_TICKS, GameMicroGames.getPlugin());
  }

  private void removeCooldown(User user) {
    user.clearArmor();
  }

  private void removeHotPotatoFrom(User user) {
    this.holders.remove(user);
    user.playNote(Instrument.PIANO, Note.natural(1, Note.Tone.C));
    user.clearInventory();


    this.giveCooldown(user);
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
        MicroGamesServer.broadcastMicroGamesTDMessage("§pThe best player is now §cglowing!");
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
      if (this.holders.isEmpty()) {
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

    if (this.cooldownUsers.contains(target)) {
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


