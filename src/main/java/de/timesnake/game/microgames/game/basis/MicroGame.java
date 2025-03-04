/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.user.scoreboard.SideboardBuilder;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.bukkit.util.world.ExWorld.Restriction;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public abstract class MicroGame implements MicroGameExtensionBase, Listener {

  protected static final Integer SPEC_LOCATION_INDEX = 0;
  protected static final Integer START_LOCATION_INDEX = 1;
  protected static final Integer SPAWN_LOCATION_INDEX = 2;

  public static final java.util.Map<Integer, Integer> PARTY_POINTS = java.util.Map.of(
      1, 6,
      2, 4,
      3, 3,
      4, 1,
      5, 1
  );

  protected final Logger logger = LogManager.getLogger("micro-game.game");

  protected final String name;
  protected final String displayName;
  protected final Material material;
  protected final String headLine;
  protected final List<String> description;
  private final int maxTimeSec;

  protected final Integer minPlayers;
  protected final List<Map> maps = new ArrayList<>();
  protected final Random random = new Random();
  protected Map currentMap;
  protected Map previousMap;
  protected Sideboard sideboard;

  private boolean isGameRunning = false;
  private Integer votes = 0;

  private BukkitTask timeTask;
  private final BossBar timeBar;
  private int timeSec;
  private int currentPlace = 1;

  public MicroGame(String name, String displayName, Material material, String headLine, List<String> description,
                   Integer minPlayers, Duration maxDuration) {
    this.name = name;
    this.displayName = displayName;
    this.material = material;
    this.headLine = headLine;
    this.description = description;
    this.minPlayers = minPlayers;
    this.maxTimeSec = maxDuration != null ? ((int) maxDuration.toSeconds()) : -1;
    this.timeBar = Server.createBossBar("Time left: §c§l" + Chat.getTimeString(timeSec), BarColor.WHITE,
        BarStyle.SOLID);

    this.beforeMapInit();

    for (Map map : MicroGamesServer.getGame().getMaps()) {
      if (map.getProperty("type") == null) {
        this.logger.warn("Can not load map '{}', no type defined", map.getName());
        continue;
      }

      if (map.getProperty("type").equals(this.name)) {
        if (map.getWorld() == null) {
          this.logger.warn("Can not load map '{}', world not exists", map.getName());
          continue;
        }

        this.maps.add(map);
        this.onMapInit(map);

        this.logger.info("Added map '{}' to game '{}'", map.getName(), this.displayName);
      }
    }

    this.sideboard = Server.getScoreboardManager()
        .registerSideboard(new SideboardBuilder()
            .name(name)
            .title("§6§l" + displayName));

    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public void beforeMapInit() {

  }

  @Override
  public void onMapInit(Map map) {
    ExWorld world = map.getWorld();

    world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
    world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
    world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
    world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
    world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    world.restrict(ExWorld.Restriction.FOOD_CHANGE, true);
    world.restrict(ExWorld.Restriction.FIRE_SPREAD_SPEED, 0f);
    world.restrict(Restriction.ENTITY_BLOCK_BREAK, true);
    world.setExceptService(true);
    world.setTime(1000);
    world.setAutoSave(false);
  }

  @Override
  public void prepare() {
    this.previousMap = this.currentMap;

    if (MicroGamesServer.getCurrentGame().equals(this)) {
      List<Map> availableMaps = new ArrayList<>(this.maps);
      availableMaps.remove(this.currentMap);
      this.currentMap = availableMaps.get(this.random.nextInt(availableMaps.size()));
    } else {
      this.currentMap = maps.get(this.random.nextInt(this.maps.size()));
    }
  }

  @Override
  public void load() {
    this.timeSec = this.maxTimeSec;
    this.timeBar.setTitle("Time left: §c§l" + Chat.getTimeString(this.timeSec));
    this.timeBar.setProgress(1);

    for (User user : Server.getPreGameUsers()) {
      user.teleport(this.getSpawnLocation());
      if (this.sideboard != null) {
        user.setSideboard(this.sideboard);
      }

      user.clearBossBars();
      if (this.maxTimeSec >= 0) {
        user.setBossBar(this.timeBar);
      }
    }

    Server.broadcastTitle(
        Component.text(this.displayName, ExTextColor.GOLD, TextDecoration.BOLD),
        Component.text(this.headLine), Duration.ofSeconds(5));

    Server.runTaskLaterSynchrony(this::applyBeforeStart, 5 * 20, GameMicroGames.getPlugin());
  }

  @Override
  public void applyBeforeStart() {
    Server.getPreGameUsers().forEach(u -> u.teleport(this.getStartLocation()));
  }

  @Override
  public void start() {
    for (User user : Server.getPreGameUsers()) {
      user.setStatus(Status.User.IN_GAME);
    }

    this.isGameRunning = true;
    MicroGamesServer.broadcastMicroGamesMessage(
        Component.text("Game started", ExTextColor.WARNING));

    if (this.maxTimeSec >= 0) {
      this.timeTask = Server.runTaskTimerSynchrony(time -> {
        this.timeBar.setTitle("Time left: §c§l" + Chat.getTimeString(time));
        this.timeBar.setProgress(time / ((double) this.maxTimeSec));
        if (time == 0) {
          this.stop();
        }
      }, this.timeSec, true, 0, 20, GameMicroGames.getPlugin());
    }
  }

  @Override
  public void addWinner(MicroGamesUser user, boolean firstWins) {
    if (!user.getStatus().equals(Status.User.OUT_GAME)) {
      user.joinSpectator();
    }

    int users = Server.getInGameUsers().size();

    if (user.hasPlace()) {
      return;
    }

    if (firstWins) {
      user.setPlace(this.currentPlace);
      MicroGamesServer.broadcastMicroGamesTDMessage(user.getTDChatName() + "§h finished #" + this.currentPlace);
      this.currentPlace++;
    } else {
      user.setPlace(users + 1);
      MicroGamesServer.broadcastMicroGamesTDMessage(user.getTDChatName() + "§h finished #" + (users + 1));
    }

    if (users == 1) {
      MicroGamesUser lastUser = (MicroGamesUser) Server.getInGameUsers().iterator().next();

      if (firstWins) {
        lastUser.setPlace(this.currentPlace);
        this.currentPlace++;
      } else {
        lastUser.setPlace(1);
      }

      if (!lastUser.getStatus().equals(Status.User.OUT_GAME)) {
        lastUser.joinSpectator();
      }
    }

    if (users <= 1) {
      this.stop();
    }

  }

  @Override
  public void addRemainingAsWinner(boolean firstWins) {
    for (User user : Server.getInGameUsers()) {
      ((MicroGamesUser) user).setPlace(firstWins ? this.currentPlace : 1);
    }
  }

  @Override
  public void stop() {
    if (!this.isGameRunning) {
      return;
    }

    this.isGameRunning = false;

    if (this.timeTask != null) {
      this.timeTask.cancel();
    }

    MicroGamesServer.getSpectatorManager().clearTools();

    MicroGamesServer.broadcastMicroGamesMessage(Chat.getLineSeparator());

    StringBuilder title = new StringBuilder();
    StringBuilder title2 = new StringBuilder();
    StringBuilder title3 = new StringBuilder();

    List<MicroGamesUser> users = Server.getUsers().stream()
        .map(u -> ((MicroGamesUser) u))
        .filter(MicroGamesUser::hasPlace)
        .sorted(Comparator.comparingInt(MicroGamesUser::getPlace))
        .toList();

    for (MicroGamesUser user : users) {
      switch (user.getPlace()) {
        case 1 -> title.append(user.getTDChatName()).append(" ");
        case 2 -> title2.append(user.getTDChatName()).append(" ");
        case 3 -> title3.append(user.getTDChatName()).append(" ");
      }

      if (MicroGamesServer.isPartyMode()) {
        user.addPoints(PARTY_POINTS.getOrDefault(user.getPlace(), 0));
      }

      MicroGamesServer.getTablistManager().getTablist().updateEntryValue(user, user.getPoints());

      MicroGamesServer.broadcastMicroGamesTDMessage(this.getWinMessage(user, user.getPlace()));
    }

    if (!title.isEmpty()) {
      title.append("§fwins");
      if (!title2.isEmpty()) {
        title2.insert(0, "2. ");
        if (!title3.isEmpty()) {
          title2.append("§f   3. ").append(title3);
        }
      }
      Server.broadcastTDTitle(title.toString(), title2.toString(), Duration.ofSeconds(3));
    } else {
      MicroGamesServer.broadcastMicroGamesTDMessage("Game ended");
      Server.broadcastTDTitle("§pGame ended", "", Duration.ofSeconds(3));
    }

    MicroGamesServer.broadcastMicroGamesMessage(Chat.getLineSeparator());

    this.previousMap = this.currentMap;

    Server.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 2);

    this.currentPlace = 1;
    Server.getUsers().forEach(u -> ((MicroGamesUser) u).resetPlace());

    MicroGamesServer.nextGame();
  }

  @Override
  public String getWinMessage(MicroGamesUser user, int place) {
    return "§6§l" + place + ". " + user.getTDChatName();
  }

  @Override
  public void reset() {
    if (this.previousMap != null) {
      Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
    }
  }

  @Override
  public boolean hasSideboard() {
    return false;
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

  @Override
  public ExLocation getSpecLocation() {
    return this.currentMap.getLocation(SPEC_LOCATION_INDEX);
  }

  @Override
  public ExLocation getStartLocation() {
    return this.currentMap.getLocation(START_LOCATION_INDEX);
  }

  @Override
  public ExLocation getSpawnLocation() {
    return this.currentMap.getLocation(SPAWN_LOCATION_INDEX);
  }

  @Override
  public boolean isGameRunning() {
    return isGameRunning;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String getHeadLine() {
    return headLine;
  }

  @Override
  public List<String> getDescription() {
    return description;
  }

  @Override
  public Sideboard getSideboard() {
    return sideboard;
  }

  @Override
  public List<Map> getMaps() {
    return maps;
  }

  @Override
  public Integer getMinPlayers() {
    return minPlayers;
  }

  @Override
  public Map getCurrentMap() {
    return currentMap;
  }

  @Override
  public Integer getVotes() {
    return votes;
  }

  @Override
  public void resetVotes() {
    this.votes = 0;
  }

  @Override
  public void addVote() {
    this.votes++;
  }

  @Override
  public void removeVote() {
    this.votes--;
  }

  @Override
  public Material getMaterial() {
    return material;
  }

  @Override
  public Random getRandom() {
    return random;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MicroGame microGame = (MicroGame) o;
    return Objects.equals(name, microGame.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }
}
