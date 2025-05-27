/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.bukkit.util.world.ExWorldOption;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.BoxedScoreGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fire;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class Firefighter extends BoxedScoreGame<Integer> implements Listener {

  private static final double FIRE_CHANCE = 0.15;

  private static final Set<Material> EXCLUDED_MATERIALS = Set.of(Material.SHORT_GRASS, Material.TALL_GRASS);

  public Firefighter() {
    super("firefighter",
        "Firefighter",
        Material.BLAZE_POWDER,
        "Punch out the fire",
        List.of("§hGoal: §pmost punched out fires", "Punch out fires by clicking left."),
        1,
        Duration.ofSeconds(45));
  }

  @Override
  public void onMapInit(Map map) {
    super.onMapInit(map);

    ExWorld world = map.getWorld();

    world.setOption(ExWorldOption.ALLOW_FIRE_PUNCH_OUT, true);
    world.setOption(ExWorldOption.ALLOW_FLINT_AND_STEEL, false);
    world.setOption(ExWorldOption.BLOCK_BURN_UP, true);
    world.setOption(ExWorldOption.ENABLE_PLAYER_DAMAGE, true);
    world.setOption(ExWorldOption.ALLOW_BLOCK_BREAK, false);
    world.setOption(ExWorldOption.ALLOW_BLOCK_PLACE, false);
    world.setOption(ExWorldOption.ALLOW_LIGHT_UP_INTERACTION, true);
    world.setOption(ExWorldOption.FIRE_SPREAD_SPEED, 0f);
    world.setOption(ExWorldOption.ALLOW_DROP_PICK_ITEM, false);
    world.setPVP(false);
  }

  @Override
  public void applyBeforeStart() {
    super.applyBeforeStart();
    this.spreadFire();
  }

  private void spreadFire() {
    ExWorld world = this.currentMap.getWorld();

    for (ExBlock block : this.getBlocksWithinBox()) {
      if (!block.isBurnable() || EXCLUDED_MATERIALS.contains(block.getType())) {
        continue;
      }

      for (BlockFace face : ExBlock.ADJACENT_BLOCK_FACINGS) {

        ExBlock adjacentBlock = block.getExRelative(face);

        if (!adjacentBlock.isEmpty()) {
          continue;
        }

        adjacentBlock.setType(Material.FIRE);

        if (world.getRandom().nextFloat() < FIRE_CHANCE) {
          adjacentBlock.setType(Material.FIRE);
          BlockData blockData = adjacentBlock.getBlockData();
          if (blockData instanceof Fire && face != BlockFace.UP) {
            ((Fire) blockData).setFace(face, true);
            adjacentBlock.setBlockData(blockData);
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
  }

  @Override
  public void stop() {
    super.calcPlaces(true);
    super.stop();
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
