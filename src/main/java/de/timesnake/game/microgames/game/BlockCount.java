/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.server.ExTime;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.UserChatCommandListener;
import de.timesnake.basic.bukkit.util.user.event.UserChatCommandEvent;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.RandomList;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.UserMap;
import de.timesnake.library.chat.Plugin;
import org.bukkit.Material;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class BlockCount extends ScoreGame<Integer> {

  private static final int SHAPE_LOCATION_INDEX = 3;

  private static final int ROUNDS = 5;
  private static final ExTime ROUND_DURATION = ExTime.ofSeconds(7);

  private int currentTimePlace = 1;
  private final UserMap<MicroGamesUser, Tuple<Integer, Integer>> guessAndTimeByUser = new UserMap<>();

  public BlockCount() {
    super("block_count", "Block Count", Material.RED_CONCRETE,
        "Guess or count", List.of(), 1, null);
  }

  @Override
  public void load() {
    super.load();
  }

  @Override
  public void start() {
    super.start();

    RandomList<Shape> shapes = RandomList.nOf(ROUNDS, Shape.SHAPES);
    final Tuple<Shape, Integer> shape = new Tuple<>(null, null);
    Server.runTaskLoopSynchrony(i -> {
      for (User user : Server.getInGameUsers()) {
        if (shape.getA() != null) {
          this.showResults(shape.getB());
          shape.getA().place(this.getShapeLocation().getExBlock(), Material.AIR);
        }
        this.guessAndTimeByUser.clear();
        this.currentTimePlace = 1;
        shape.setA(shapes.popAny());
        shape.setB(shape.getA().place(this.getShapeLocation().getExBlock(), Material.RED_CONCRETE));
        Server.getUserEventManager().addUserChatCommand(user, new EstimateCmd());
      }
    }, this::stop, ExTime.ofSeconds(3), ROUND_DURATION, ROUNDS, GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {
    super.stop();
    this.calcPlaces(true);
  }

  private void showResults(int amount) {
    List<Map.Entry<MicroGamesUser, Tuple<Integer, Integer>>> winner = this.guessAndTimeByUser.entrySet().stream()
        .sorted(Comparator.comparingInt((Map.Entry<?, Tuple<Integer, Integer>> e) -> e.getValue().getA())
            .thenComparingInt(e -> e.getValue().getB()))
        .toList();

    MicroGamesServer.broadcastMicroGamesTDMessage("§hBlocks: §v" + amount);
    for (int i = 0; i < winner.size(); i++) {
      Map.Entry<MicroGamesUser, Tuple<Integer, Integer>> entry = winner.get(i);
      MicroGamesServer.broadcastMicroGamesTDMessage("§p" + (i + 1) + ". " + entry.getKey().getTDChatName() + " §7(" + entry.getValue().getA() + ")");
      if (i < winner.size() / 2) {
        this.updateUserScore(entry.getKey(), (u, score) -> score + 1);
      }
    }
  }

  @Override
  public void reset() {
    super.reset();

    this.guessAndTimeByUser.clear();
    this.currentTimePlace = 1;
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  public ExLocation getShapeLocation() {
    return this.currentMap.getLocation(SHAPE_LOCATION_INDEX);
  }

  private interface Shape {

    Shape FILLED_CUBE = (center, material) -> {
      int sizeX = Server.getRandom().nextInt(8, 16);
      int sizeY = Server.getRandom().nextInt(8, 16);
      int sizeZ = Server.getRandom().nextInt(8, 16);

      for (int x = 0; x < sizeX; x++) {
        for (int z = 0; z < sizeZ; z++) {
          for (int y = 0; y < sizeY; y++) {
            center.getRelative(x, y, z).setType(material);
          }
        }
      }

      return sizeX * sizeY * sizeZ;
    };

    Shape SCATTERED_CUBE = (center, material) -> {
      int sizeX = Server.getRandom().nextInt(8, 16);
      int sizeY = Server.getRandom().nextInt(8, 16);
      int sizeZ = Server.getRandom().nextInt(8, 16);
      float chance = Server.getRandom().nextFloat(0.2f, 0.8f);
      int blocks = 0;

      for (int x = 0; x < sizeX; x++) {
        for (int z = 0; z < sizeZ; z++) {
          for (int y = 0; y < sizeY; y++) {
            if (Server.getRandom().nextFloat(1) < chance) {
              center.getRelative(x, y, z).setType(material);
              blocks++;
            }
          }
        }
      }
      return blocks;
    };

    Shape SLICE = (center, material) -> {
      int sizeX = Server.getRandom().nextInt(10, 18);
      int sizeY = Server.getRandom().nextInt(10, 18);
      int sizeZ = Server.getRandom().nextInt(10, 18);
      int blocks = 0;

      for (int x = 0; x < sizeX; x++) {
        for (int y = 0; y < sizeY; y++) {
          for (int z = x / 2; z < sizeZ - x / 2; z++) {
            center.getRelative(x, y, z).setType(material);
            blocks++;
          }
        }
      }
      return blocks;
    };

    Shape PYRAMIDE = (center, material) -> {
      int sizeXZ = Server.getRandom().nextInt(8, 16);
      int sizeY = Server.getRandom().nextInt(8, 20);
      int baseXZ = 0;
      int blocks = 0;

      for (int y = 0; y < sizeY; y++) {
        for (int x = baseXZ; x < sizeXZ; x++) {
          for (int z = baseXZ; z < sizeXZ; z++) {
            center.getRelative(x, y, z).setType(material);
            blocks++;
          }
        }
        baseXZ++;
        sizeXZ--;
      }
      return blocks;
    };

    RandomList<Shape> SHAPES = RandomList.of(FILLED_CUBE, SCATTERED_CUBE, SLICE, PYRAMIDE);

    int place(ExBlock center, Material material);

  }

  private class EstimateCmd implements UserChatCommandListener {

    @Override
    public void onUserChatCommand(UserChatCommandEvent event) {
      if (!BlockCount.this.isGameRunning()) {
        return;
      }

      MicroGamesUser user = ((MicroGamesUser) event.getUser());

      int value;
      try {
        value = Integer.parseInt(event.getMessage());
      } catch (NumberFormatException e) {
        user.sendPluginTDMessage(Plugin.GAME, "§wNot a number");
        return;
      }

      BlockCount.this.guessAndTimeByUser.put(user, new Tuple<>(value, BlockCount.this.currentTimePlace++));
      user.sendPluginTDMessage(Plugin.GAME, "§sYour guess: §v" + value);
      MicroGamesServer.broadcastMicroGamesTDMessage(user.getTDChatName() + "§p has guessed");
      event.removeListener(true);
      event.setCancelled(true);
    }
  }
}
