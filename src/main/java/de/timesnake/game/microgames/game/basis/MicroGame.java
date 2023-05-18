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
import de.timesnake.library.basic.util.Loggers;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Chat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitTask;

public abstract class MicroGame {

  public static final int FIRST_POINTS = 3;
  public static final int SECOND_POINTS = 2;
  public static final int THIRD_POINTS = 1;

  protected final String name;
  protected final String displayName;
  protected final Material material;
  protected final String description;
  private final int maxTimeSec;

  protected final Integer minPlayers;
  protected final List<Map> maps = new ArrayList<>();
  protected final Random random = new Random();
  protected Map currentMap;
  protected Map previousMap;
  protected Sideboard sideboard;


  protected LinkedList<MicroGamesUser> placement = new LinkedList<>();

  private boolean isGameRunning = false;
  private Integer votes = 0;

  private BukkitTask timeTask;
  private final BossBar timeBar;
  private int timeSec;

  public MicroGame(String name, String displayName, Material material, String description,
      Integer minPlayers, int maxTimeSec) {
    this.name = name;
    this.displayName = displayName;
    this.material = material;
    this.description = description;
    this.minPlayers = minPlayers;
    this.maxTimeSec = maxTimeSec;
    this.timeBar = Server.createBossBar("Time left: §c§l" + Chat.getTimeString(timeSec),
        BarColor.WHITE, BarStyle.SOLID);

    for (Map map : MicroGamesServer.getGame().getMaps()) {
      if (map.getInfo().get(0).equalsIgnoreCase(this.name)) {
        if (map.getWorld() == null) {
          Loggers.GAME.warning("Can not load map " + map.getName() +
              ", world not exists");
          continue;
        }

        if (map.getLocations().size() < this.getLocationAmount()) {
          Loggers.GAME.warning("Can not load map " + map.getName() + ", too few " +
              "locations");
          continue;
        }

        this.maps.add(map);
        this.onMapLoad(map);

        Loggers.GAME.info("Added map " + map.getName() + " to game " + this.displayName);
      }
    }

    this.sideboard = Server.getScoreboardManager()
        .registerSideboard(new SideboardBuilder()
            .name(name)
            .title("§6§l" + displayName));
  }

  public void onMapLoad(Map map) {
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

  public abstract Integer getLocationAmount();

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

  public void load() {
    this.timeSec = this.maxTimeSec;
    this.timeBar.setTitle("Time left: §c§l" + Chat.getTimeString(this.timeSec));
    this.timeBar.setProgress(1);

    for (User user : Server.getPreGameUsers()) {
      user.teleport(this.getStartLocation());
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
        Component.text(this.description), Duration.ofSeconds(5));

    Server.runTaskLaterSynchrony(this::loadDelayed, 5 * 20, GameMicroGames.getPlugin());
  }

  protected abstract void loadDelayed();

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

  protected void addWinner(MicroGamesUser user, boolean first) {
    if (!user.getStatus().equals(Status.User.OUT_GAME)) {
      user.joinSpectator();
    }

    int users = Server.getInGameUsers().size();

    if (this.placement.contains(user)) {
      return;
    }

    if (first) {
      this.placement.addLast(user);
      MicroGamesServer.broadcastMicroGamesTDMessage(user.getTDChatName() + "§w finished #"
          + this.placement.size());
    } else {
      this.placement.addFirst(user);
      MicroGamesServer.broadcastMicroGamesTDMessage(user.getTDChatName() + "§w finished #"
          + (users + 1));
    }

    if (users == 1) {
      MicroGamesUser lastUser = (MicroGamesUser) Server.getInGameUsers().iterator().next();

      if (first) {
        this.placement.addLast(lastUser);
      } else {
        this.placement.addFirst(lastUser);
      }

      if (!lastUser.getStatus().equals(Status.User.OUT_GAME)) {
        lastUser.joinSpectator();
      }
    }

    if (users <= 1) {
      this.stop();
    }

  }

  public void stop() {
    if (!this.isGameRunning) {
      return;
    }

    this.isGameRunning = false;

    if (this.timeTask != null) {
      this.timeTask.cancel();
    }

    MicroGamesServer.broadcastMicroGamesMessage(Chat.getLineSeparator());

    int place = 1;
    String title = null;
    StringBuilder subTitle = new StringBuilder();

    for (MicroGamesUser user : this.placement) {
      if (place == 1) {
        if (MicroGamesServer.isPartyMode()) {
          user.addPoints(FIRST_POINTS);
        }
        title = user.getTDChatName() + "§f wins";
      } else if (place == 2) {
        if (MicroGamesServer.isPartyMode()) {
          user.addPoints(SECOND_POINTS);
        }
        subTitle.append("2. ").append(user.getTDChatName());
      } else if (place == 3) {
        if (MicroGamesServer.isPartyMode()) {
          user.addPoints(THIRD_POINTS);
        }
        subTitle.append("§f    3. ").append(user.getTDChatName());
      }

      MicroGamesServer.getTablistManager().getTablist()
          .updateEntryValue(user, user.getPoints());

      MicroGamesServer.broadcastMicroGamesTDMessage(this.getWinMessage(user, place));
      place++;
    }

    if (title != null) {
      Server.broadcastTDTitle(title, subTitle.toString(), Duration.ofSeconds(3));
    } else {
      MicroGamesServer.broadcastMicroGamesTDMessage("Game ended");
      Server.broadcastTitle(Component.text("Game ended", ExTextColor.WHITE),
          Component.empty(), Duration.ofSeconds(3));
    }

    MicroGamesServer.broadcastMicroGamesMessage(Chat.getLineSeparator());

    this.previousMap = this.currentMap;

    Server.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 2);

    MicroGamesServer.nextGame();
  }

  protected String getWinMessage(MicroGamesUser user, int place) {
    return "§6§l" + place + ". " + user.getTDChatName();
  }

  public void reset() {
    this.placement.clear();
  }

  public abstract boolean hasSideboard();

  public abstract boolean onUserJoin(MicroGamesUser user);

  public abstract void onUserQuit(MicroGamesUser user);

  public abstract ExLocation getSpecLocation();

  public abstract ExLocation getStartLocation();

  public boolean isGameRunning() {
    return isGameRunning;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public Sideboard getSideboard() {
    return sideboard;
  }

  public List<Map> getMaps() {
    return maps;
  }

  public Integer getMinPlayers() {
    return minPlayers;
  }

  public Integer getVotes() {
    return votes;
  }

  public void resetVotes() {
    this.votes = 0;
  }

  public void addVote() {
    this.votes++;
  }

  public void removeVote() {
    this.votes--;
  }

  public Material getMaterial() {
    return material;
  }

}
