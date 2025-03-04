/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.server.ExTime;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.world.BlockPolygon;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.game.extension.ArenaGame;
import de.timesnake.game.microgames.game.extension.RandomStartLocation;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.library.basic.util.RandomList;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PuddleJumping extends ScoreGame<Integer> implements ArenaGame, RandomStartLocation {

  private static final int MAX_PUDDLE_SIZE = 3;
  private static final float PUDDLE_GENERATION_TRIES_PER_BLOCK = 0.03f;
  private static final ExTime TELEPORT_DELAY = ExTime.ofSeconds(2);

  private final HashMap<User, BukkitTask> teleportTasks = new HashMap<>();
  private final HashMap<ExBlock, Integer> puddleSizeByBlock = new HashMap<>();

  private BlockPolygon arena;

  public PuddleJumping() {
    super("puddle_jumping", "Puddle Jumping", Material.WATER_BUCKET,
        "Jump and survive", List.of(), 1, Duration.ofSeconds(90));
  }

  @Override
  public void onMapInit(Map map) {
    super.onMapInit(map);
    map.getWorld().restrict(ExWorld.Restriction.NO_PLAYER_DAMAGE, true);
  }

  @Override
  public void prepare() {
    super.prepare();
    this.arena = this.getArena();
    this.puddleSizeByBlock.clear();
    this.teleportTasks.clear();
    this.generatePuddles();
  }

  @Override
  public void applyBeforeStart() {
    super.applyBeforeStart();
    Server.getPreGameUsers().forEach(User::lockLocation);
  }

  @Override
  public void start() {
    super.start();
    Server.getPreGameUsers().forEach(User::unlockLocation);
  }

  @Override
  public void stop() {
    this.teleportTasks.forEach((u, t) -> t.cancel());
    this.calcPlaces(true);
    super.stop();
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  private void generatePuddles() {
    RandomList<ExBlock> blocks = new RandomList<>(this.arena.getBlocksInsideOnMinHeight(b -> !b.isEmpty()));

    int tries = (int) (blocks.size() * PUDDLE_GENERATION_TRIES_PER_BLOCK);

    for (int size = MAX_PUDDLE_SIZE; size > 0; size--) {
      for (int i = 0; i < tries / size; i++) {
        ExBlock block = blocks.popAny();
        if (this.isValidPuddleBlock(block, size)) {
          this.createPuddle(block, size);
        }
      }
    }
  }

  private boolean isValidPuddleBlock(ExBlock block, int puddleSize) {
    for (int x = 0; x < puddleSize + 2; x++) {
      for (int z = 0; z < puddleSize + 2; z++) {
        ExBlock b = block.getExRelative(x, 0, z);
        if (!this.arena.contains(b) || b.getType().equals(Material.WATER)) {
          return false;
        }
      }
    }
    return true;
  }

  private void createPuddle(ExBlock block, int puddleSize) {
    for (int x = 0; x < puddleSize; x++) {
      for (int z = 0; z < puddleSize; z++) {
        ExBlock b = block.getExRelative(x + 1, 0, z + 1);
        b.setType(Material.WATER);
        this.puddleSizeByBlock.put(b, puddleSize);
      }
    }
  }

  private void removePuddle(ExBlock block, Set<ExBlock> seenBlocks) {
    if (seenBlocks.contains(block)) {
      return;
    }
    seenBlocks.add(block);
    if (!block.getType().equals(Material.WATER)) {
      return;
    }
    block.editBlockData(b -> ((Levelled) b).setLevel(1));
    block.getNeighborBlocks("x,z,xz").forEach(b -> this.removePuddle(b, seenBlocks));
  }

  @EventHandler
  public void onUserMove(UserMoveEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    User user = e.getUser();

    if (user.getVelocity().lengthSquared() > 0.1 || this.teleportTasks.containsKey(user)) {
      return;
    }

    ExBlock block = ExBlock.fromBlock(e.getTo().getBlock());

    if (block.getY() <= this.getArena().getMinHeight() + 1) {
      Integer puddleSize = this.puddleSizeByBlock.remove(block);
      if (puddleSize != null) {
        this.removePuddle(block, new HashSet<>());
        int points = (MAX_PUDDLE_SIZE - puddleSize + 1);
        this.updateUserScore(user, (u, v) -> v + points);
        user.playNote(Instrument.BIT, Note.natural(1, Note.Tone.C));
        user.showTDTitle("§y+" + points, "", Duration.ofSeconds(1));
      } else {
        this.updateUserScore(user, (u, v) -> v - 1);
        user.playNote(Instrument.BIT, Note.natural(0, Note.Tone.C));
        user.showTDTitle("§z-1", "", Duration.ofSeconds(1));
      }

      this.teleportTasks.put(user, Server.runTaskLaterSynchrony(() -> {
        user.teleport(this.getStartLocation());
        this.teleportTasks.remove(user);
      }, TELEPORT_DELAY.toTicks(), GameMicroGames.getPlugin()));
    }
  }
}
