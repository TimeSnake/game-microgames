/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.UserChatCommandListener;
import de.timesnake.basic.bukkit.util.user.event.UserChatCommandEvent;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.BoxedScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.RandomList;
import de.timesnake.library.basic.util.UserMap;
import de.timesnake.library.chat.Plugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class AnimalFarm extends BoxedScoreGame<Integer> {

  private static final int ROUNDS = 5;
  private static final int MIN_ANIMALS_PER_TYPE = 3;
  private static final int MAX_ANIMALS_PER_TYPE = 16;
  private static final int MIN_ANIMAL_TYPES = 3;
  private static final int MAX_ANIMAL_TYPES = 6;

  private static final RandomList<EntityType> ANIMAL_TYPES = RandomList.of(EntityType.COW, EntityType.SHEEP,
      EntityType.CHICKEN,
      EntityType.PIG, EntityType.DONKEY, EntityType.HORSE, EntityType.BEE);

  private final RandomList<Block> spawnBlocks = new RandomList<>();

  private RandomList<EntityType> currentTypes;
  private final UserMap<MicroGamesUser, Integer> guessByUser = new UserMap<>();

  public AnimalFarm() {
    super("animal_farm", "Animal Farm", Material.HAY_BLOCK,
        "Count your Animals", List.of(""), 2, null);
  }

  @Override
  public void onMapInit(Map map) {
    super.onMapInit(map);

    List<Block> blocks = new ArrayList<>(this.getBlocksWithinBox());

    for (Block block : blocks) {
      if (block.getType().equals(Material.GRASS_BLOCK) && block.getRelative(BlockFace.UP).getType().isEmpty()) {
        this.spawnBlocks.add(block);
      }
    }
  }

  @Override
  public void start() {
    super.start();

    Server.runTaskAsynchrony(() -> {
      for (int i = 0; i < ROUNDS; i++) {
        this.guessByUser.clear();

        for (User user : Server.getInGameUsers()) {
          Server.getUserEventManager().addUserChatCommand(user, new EstimateCmd());
        }

        this.currentTypes = new RandomList<>(ANIMAL_TYPES);
        this.spawnAnimals(this.currentTypes.popAny(), this.random.nextInt(MIN_ANIMALS_PER_TYPE,
            MAX_ANIMALS_PER_TYPE + 1));


      }
    }, GameMicroGames.getPlugin());
  }

  private void spawnAnimals(EntityType guessType, int guessAmount) {
    this.spawnType(guessType, guessAmount);

    int numTypes = this.random.nextInt(MIN_ANIMAL_TYPES - 1, MAX_ANIMAL_TYPES);
    for (int i = 0; i < numTypes; i++) {
      EntityType type = this.currentTypes.popAny();
      int amount = this.random.nextInt(MIN_ANIMALS_PER_TYPE, MAX_ANIMALS_PER_TYPE + 1);
      this.spawnType(type, amount);
    }
  }

  private void spawnType(EntityType type, int size) {
    for (int i = 0; i < size; i++) {
      this.currentMap.getWorld().spawnEntity(this.spawnBlocks.getAny().getLocation(), type);
    }
  }

  private void computeResult(int amount) {
    int bestGuess = Integer.MIN_VALUE;
    List<MicroGamesUser> winner = new ArrayList<>();

    for (java.util.Map.Entry<MicroGamesUser, Integer> entry : this.guessByUser.entrySet()) {
      int diff = Math.abs(Math.abs(amount - entry.getValue()) - Math.abs(amount - bestGuess));
      if (diff == 0) {
        winner.add(entry.getKey());
      } else if (diff < 0) {
        winner.clear();
        winner.add(entry.getKey());
      }
    }
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  @Override
  public void reset() {
    super.reset();

    // TODO remove entities
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  private class EstimateCmd implements UserChatCommandListener {

    @Override
    public void onUserChatCommand(UserChatCommandEvent event) {
      if (!AnimalFarm.this.isGameRunning()) {
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

      AnimalFarm.this.guessByUser.put(user, value);
      user.sendPluginTDMessage(Plugin.GAME, "§sYour guess: " + value);
      MicroGamesServer.broadcastMicroGamesTDMessage(user.getTDChatName() + "§p has guessed");
      event.removeListener(true);
    }
  }

}
