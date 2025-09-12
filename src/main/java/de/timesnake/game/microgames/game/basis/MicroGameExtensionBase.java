/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;

import java.util.List;
import java.util.Random;

public interface MicroGameExtensionBase {

  void beforeMapInit();

  void onMapInit(Map map);

  void prepare();

  void load();

  void applyBeforeStart();

  void start();

  void addWinner(MicroGamesUser user, boolean firstWins);

  void addRemainingAsWinner(boolean firstWins);

  void stop();

  String getWinMessage(MicroGamesUser user, int place);

  void reset();

  boolean hasSideboard();

  boolean onUserJoin(MicroGamesUser user);

  void onUserQuit(MicroGamesUser user);

  ExLocation getSpecLocation();

  ExLocation getSpawnLocation();

  boolean isGameRunning();

  String getName();

  String getDisplayName();

  String getHeadLine();

  List<String> getDescription();

  Sideboard getSideboard();

  List<Map> getMaps();

  Integer getMinPlayers();

  Integer getVotes();

  void resetVotes();

  void addVote();

  void removeVote();

  Material getMaterial();

  Map getCurrentMap();

  Random getRandom();
}
