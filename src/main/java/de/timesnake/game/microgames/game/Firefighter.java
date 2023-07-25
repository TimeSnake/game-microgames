/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.bukkit.util.world.ExWorld.Restriction;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.BoxedScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.extension.util.chat.Chat;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fire;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Set;

public class Firefighter extends BoxedScoreGame<Integer> implements Listener {

  private static final Duration DURATION = Duration.ofSeconds(45);
  private static final double FIRE_CHANCE = 0.2;

  private static final Set<Material> EXCLUDED_MATERIALS = Set.of(Material.GRASS,
      Material.TALL_GRASS);

  private BukkitTask timeTask;

  public Firefighter() {
    super("firefighter", "Firefighter", Material.BLAZE_POWDER,
        "Punch out the fire", 1, null);

    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public Integer getLocationAmount() {
    return 5;
  }

  @Override
  public void onMapLoad(Map map) {
    super.onMapLoad(map);

    ExWorld world = map.getWorld();

    world.restrict(Restriction.FIRE_PUNCH_OUT, false);
    world.restrict(Restriction.FLINT_AND_STEEL, true);
    world.restrict(Restriction.BLOCK_BURN_UP, false);
    world.restrict(Restriction.NO_PLAYER_DAMAGE, true);
    world.restrict(Restriction.BLOCK_BREAK, true);
    world.restrict(Restriction.BLOCK_PLACE, true);
    world.restrict(Restriction.LIGHT_UP_INTERACTION, false);
    world.restrict(Restriction.FIRE_SPREAD_SPEED, 0f);
    world.restrict(Restriction.NO_PLAYER_DAMAGE, true);
    world.restrict(Restriction.DROP_PICK_ITEM, true);
    world.setPVP(false);
  }

  @Override
  public void load() {
    super.load();

    super.sideboard.setScore(4, "§9§lTime");
    super.sideboard.setScore(3, "§f" + DURATION + "s");
    super.sideboard.setScore(2, "§f-------------------");
  }

  @Override
  protected void loadDelayed() {
    super.loadDelayed();
    this.spreadFire();
  }

  private void spreadFire() {
    ExWorld world = this.currentMap.getWorld();

    for (Block block : this.getBlocksWithinBox()) {
      if (!block.isBurnable() || EXCLUDED_MATERIALS.contains(block.getType())) {
        continue;
      }

      for (Tuple<Vector, BlockFace> tuple : ExBlock.NEAR_BLOCKS_WITH_FACING) {
        Vector vec = tuple.getA();
        BlockFace blockFace = tuple.getB();

        Block nearBlock = block.getLocation().add(vec).getBlock();

        if (!nearBlock.isEmpty()) {
          continue;
        }

        if (world.getRandom().nextFloat() < FIRE_CHANCE) {
          nearBlock.setType(Material.FIRE);
          if (blockFace != BlockFace.DOWN) {
            BlockData blockData = nearBlock.getBlockData();
            if (blockData instanceof Fire) {
              ((Fire) blockData).setFace(blockFace, true);
              nearBlock.setBlockData(blockData);
            }

          }

        }
      }
    }
  }

  @Override
  public void start() {
    super.start();

    for (User user : Server.getInGameUsers()) {
      user.setGameMode(GameMode.SURVIVAL);
    }

    this.timeTask = Server.runTaskTimerSynchrony((time) -> {
      super.sideboard.setScore(3, Chat.getTimeString(time));

      if (time == 0) {
        this.stop();
      }
    }, ((int) DURATION.toSeconds()), true, 0, 20, GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {
    if (this.timeTask != null) {
      this.timeTask.cancel();
    }

    super.calcPlaces(true);
    super.stop();
  }

  @Override
  public void reset() {
    super.reset();

    if (this.previousMap != null) {
      Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
    }
  }

  @Override
  public String getScoreName() {
    return "Punched out fires";
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
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e) {
    MicroGamesUser user = (MicroGamesUser) Server.getUser(e.getPlayer());

    if (user == null || !this.isGameRunning()) {
      return;
    }

    if (!user.getStatus().equals(Status.User.IN_GAME)) {
      e.setCancelled(true);
      return;
    }

    if (e.getClickedBlock() == null || !e.getClickedBlock().getType().equals(Material.FIRE)) {
      return;
    }

    this.updateUserScore(user, (u, v) -> v == null ? 1 : v + 1);
  }


}
