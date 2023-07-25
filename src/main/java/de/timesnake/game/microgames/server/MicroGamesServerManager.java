/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.server;

import com.google.common.collect.Lists;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.*;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.user.scoreboard.Tablist;
import de.timesnake.basic.game.util.game.Game;
import de.timesnake.basic.game.util.server.GameServerManager;
import de.timesnake.basic.game.util.user.SpectatorManager;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.game.*;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.game.microgames.user.PartyManager;
import de.timesnake.game.microgames.user.TablistManager;
import de.timesnake.library.basic.util.Loggers;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.WeightedRandomCollection;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.game.NonTmpGameInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class MicroGamesServerManager extends GameServerManager<Game<NonTmpGameInfo>> implements
    Listener {

  public static MicroGamesServerManager getInstance() {
    return (MicroGamesServerManager) ServerManager.getInstance();
  }

  private static final Integer NEXT_GAME_DELAY = 15;
  private static final Integer START_DELAY = 10;
  private final Map<String, MicroGame> microGamesByName = new HashMap<>();
  private final Map<Integer, List<MicroGame>> microGamesByMinPlayers = new HashMap<>();

  private final Random random = new Random();

  private MicroGame currentGame;

  private PartyManager partyManager;
  private ListIterator<MicroGame> partyGameIterator;

  private BukkitTask delayTask;
  private BukkitTask startTask;
  private int start = NEXT_GAME_DELAY;

  private boolean paused = true;
  private boolean partyMode = false;

  private TablistManager tablistManager;

  public void onMicroGamesEnable() {
    super.onGameEnable();

    super.getGame().loadMaps(true);

    this.loadGame(new LadderKing());
    this.loadGame(new ColorSwap());
    this.loadGame(new Parkour());
    this.loadGame(new KnockOut());
    this.loadGame(new BlockJump());
    this.loadGame(new HotPotato());
    this.loadGame(new Dropper());
    this.loadGame(new BoatRace());
    this.loadGame(new TntRun());
    this.loadGame(new Firefighter());
    this.loadGame(new Spleef());
    this.loadGame(new ColorPunch());
    this.loadGame(new EggHunter());
    this.loadGame(new OreMiner());
    this.loadGame(new Sheeeep());

    this.partyManager = new PartyManager();

    this.tablistManager = new TablistManager();

    Server.registerListener(this, GameMicroGames.getPlugin());

    this.currentGame = microGamesByName.values().stream().toList().get(this.random.nextInt(microGamesByName.size()));
    this.currentGame.prepare();
    this.currentGame.load();
  }

  private void loadGame(MicroGame game) {
    if (game.getMaps().isEmpty()) {
      Loggers.GAME.info("Not loaded game '" + game.getDisplayName() + "', no map found");
      return;
    }

    this.microGamesByName.put(game.getName(), game);
    this.microGamesByMinPlayers.computeIfAbsent(game.getMinPlayers(), k -> new LinkedList<>()).add(game);
    Loggers.GAME.info("Loaded game " + game.getDisplayName());
  }

  @Override
  public User loadUser(Player player) {
    return new MicroGamesUser(player);
  }

  @Override
  public Sideboard getGameSideboard() {
    return MicroGamesServer.getCurrentGame().getSideboard();
  }

  @Override
  public Tablist getGameTablist() {
    return MicroGamesServer.getTablistManager().getTablist();
  }

  @Override
  protected Game<NonTmpGameInfo> loadGame(DbGame dbGame, boolean loadWorlds) {
    return super.loadGame(dbGame, true);
  }

  @Override
  protected SpectatorManager initSpectatorManager() {
    return new de.timesnake.game.microgames.user.SpectatorManager();
  }

  public void pause() {
    this.paused = true;
  }

  public void nextGame() {
    Server.getInGameUsers().forEach(u -> u.addCoins(MicroGamesServer.GAME_COINS, true));

    if (this.partyMode) {
      this.nextPartyGame();
      return;
    }

    MicroGame votedGame;

    WeightedRandomCollection<MicroGame> votedGames = new WeightedRandomCollection<>(this.random);

    votedGames.addAll(this.microGamesByName.values().stream()
            .filter(mg -> Server.getGameNotServiceUsers().size() >= mg.getMinPlayers() && (mg.getMaps().size() > 1 || !mg.equals(this.currentGame)))
            .toList(),
        mg -> mg.getVotes().doubleValue());

    if (votedGames.size() == 0) {
      votedGame = this.getRandomGame();
    } else {
      votedGame = votedGames.next();
    }

    if (votedGame == null) {
      this.broadcastMicroGamesTDMessage("§wNo game found, waiting for more players");
      this.paused = true;
      return;
    }

    this.paused = false;

    this.startNextGame(votedGame);
  }

  private void startNextGame(MicroGame nextGame) {

    for (User user : Server.getInGameUsers()) {
      ((MicroGamesUser) user).joinSpectator();
    }

    this.broadcastMicroGamesTDMessage("§wSwitching to §v" + nextGame.getDisplayName());
    nextGame.prepare();

    this.delayTask = Server.runTaskLaterSynchrony(() -> {
      for (User user : Server.getGameUsers()) {
        user.resetSideboard();
      }

      for (User user : Server.getGameNotServiceUsers()) {
        ((MicroGamesUser) user).joinGame();
      }

      nextGame.load();

      final MicroGame lastGame = this.currentGame;

      if (lastGame != null) {
        Server.runTaskLaterSynchrony(lastGame::reset, 20 * 5, GameMicroGames.getPlugin());
      }

      this.currentGame = nextGame;

      if (Server.getPreGameUsers().size() == 0 || this.paused) {
        this.paused = true;
        Loggers.GAME.info("Paused game loop");
        return;
      }

      this.start = START_DELAY;

      this.startTask = Server.runTaskTimerSynchrony(() -> {
        if (start == 0) {
          if (Server.getPreGameUsers().size() == 0 || this.paused) {
            this.paused = true;
            Loggers.GAME.info("Paused game loop");
            this.startTask.cancel();
            return;
          }
          Server.broadcastNote(Instrument.PLING, Note.natural(0, Note.Tone.A));
          this.currentGame.start();
          this.startTask.cancel();
        } else if (start <= 5) {
          Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
          MicroGamesServer.broadcastMicroGamesTDMessage("§pThe game starts in §v" + start + " §ps");
          Server.broadcastTDTitle("§c" + start, "", Duration.ofSeconds(1));
        }
        start--;
      }, 0, 20, GameMicroGames.getPlugin());

    }, NEXT_GAME_DELAY * 20, GameMicroGames.getPlugin());
  }

  private MicroGame getRandomGame() {
    MicroGame nextGame;
    do {
      int players = Server.getGameNotServiceUsers().size();
      List<MicroGame> games = new ArrayList<>();
      for (Map.Entry<Integer, List<MicroGame>> entry : this.microGamesByMinPlayers.entrySet()) {
        if (players >= entry.getKey()) {
          games.addAll(entry.getValue());
        }
      }
      if (games.size() == 0) {
        return null;
      }

      nextGame = games.get(this.random.nextInt(games.size()));

    } while (nextGame.getMaps().size() == 1 && this.currentGame.equals(nextGame));
    return nextGame;
  }

  public void startParty() {
    if (this.currentGame.isGameRunning() || this.partyMode) {
      return;
    }

    if (this.delayTask != null) {
      this.delayTask.cancel();
    }

    this.broadcastMicroGamesTDMessage("§cStarting party mode!");
    this.broadcastMicroGamesTDMessage(Chat.getLineTDSeparator());
    this.broadcastMicroGamesTDMessage("§hPoint Distribution:");

    for (Map.Entry<Integer, Integer> points : MicroGame.PARTY_POINTS.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).toList()) {
      this.broadcastMicroGamesTDMessage("§p    " + points.getKey() + ". Place: §v" + points.getValue() + " points");
    }

    this.broadcastMicroGamesMessage(Chat.getLineSeparator());
    Server.broadcastTDTitle("§wParty Mode", "§pEarn points by winning a game. The player with most points wins",
        Duration.ofSeconds(5));
    Server.broadcastNote(Instrument.BELL, Note.natural(0, Note.Tone.C));

    this.partyMode = true;

    for (User user : Server.getUsers()) {
      ((MicroGamesUser) user).resetPoints();
    }

    List<MicroGame> partyGames = new ArrayList<>();

    int players = Server.getGameNotServiceUsers().size();

    for (Map.Entry<Integer, List<MicroGame>> entry : this.microGamesByMinPlayers.entrySet()) {
      if (players >= entry.getKey()) {
        partyGames.addAll(entry.getValue());
      }
    }

    while (partyGames.get(0).equals(this.currentGame)) {
      Collections.shuffle(partyGames);
    }

    this.partyGameIterator = partyGames.listIterator();

    this.nextPartyGame();
  }

  public void nextPartyGame() {
    if (this.partyGameIterator.hasNext()) {
      MicroGame nextGame = this.partyGameIterator.next();
      if (Server.getGameNotServiceUsers().size() >= nextGame.getMinPlayers()) {
        this.startNextGame(nextGame);
      } else {
        this.nextPartyGame();
      }

    } else {
      this.stopParty();
    }
  }

  public void stopParty() {

    Server.runTaskLaterSynchrony(() -> {
      List<User> users = new LinkedList<>(Server.getGameNotServiceUsers());
      users.sort(Comparator.comparingInt((user) -> ((MicroGamesUser) user).getPoints()));
      users = Lists.reverse(users);

      this.broadcastMicroGamesTDMessage("");
      this.broadcastMicroGamesTDMessage("");
      this.broadcastMicroGamesTDMessage("§wThe party has ended");
      this.broadcastMicroGamesTDMessage(Chat.getLineTDSeparator());

      Server.broadcastTDTitle(users.get(0).getTDChatName() + "§p wins", "§wThe party has ended",
          Duration.ofSeconds(4));

      int i = 1;
      for (User user : users) {
        this.broadcastMicroGamesTDMessage("§h§l " + i + ".  " + user.getTDChatName()
            + " §v(" + ((MicroGamesUser) user).getPoints() + ")");
        i++;

        ((MicroGamesUser) user).resetPoints();
        this.getTablistManager().getTablist().updateEntryValue(user, ((MicroGamesUser) user).getPoints());
      }
      this.broadcastMicroGamesTDMessage(Chat.getLineTDSeparator());

      this.partyGameIterator = null;
      this.partyMode = false;

      Server.runTaskLaterSynchrony(this::nextGame, 20 * 7, GameMicroGames.getPlugin());
    }, 5 * 20, GameMicroGames.getPlugin());

  }

  public void skipGame() {
    if (this.currentGame != null && this.currentGame.isGameRunning()) {
      this.currentGame.stop();
    }
  }

  public void broadcastMicroGamesMessage(Component message) {
    Server.broadcastMessage(Plugin.MICRO_GAMES, message);
  }

  public void broadcastMicroGamesTDMessage(String message) {
    Server.broadcastTDMessage(Plugin.MICRO_GAMES, message);
  }

  public boolean isPaused() {
    return paused;
  }

  public MicroGame getCurrentGame() {
    return currentGame;
  }

  public Collection<MicroGame> getGames() {
    return this.microGamesByName.values();
  }

  public boolean isPartyMode() {
    return partyMode;
  }

  public TablistManager getTablistManager() {
    return tablistManager;
  }

  @EventHandler
  public void onUserJoin(UserJoinEvent e) {
    MicroGamesUser user = (MicroGamesUser) e.getUser();
    if (this.paused) {
      user.joinSpectator();
      Loggers.GAME.info("Resumed game loop");
      this.nextGame();
    } else {
      if (!this.currentGame.onUserJoin(user)) {
        user.setStatus(Status.User.SPECTATOR);
        user.joinSpectator();
      }
    }
  }

  @EventHandler
  public void onUserQuit(UserQuitEvent e) {
    MicroGamesUser user = ((MicroGamesUser) e.getUser());
    if (this.currentGame.isGameRunning()) {
      this.currentGame.onUserQuit(user);
    } else if (Server.getGameNotServiceUsers().size() == 0) {
      this.startTask.cancel();
      this.paused = true;
      Loggers.GAME.info("Paused game loop");
    }

    user.clearVotes();
  }

  @EventHandler
  public void onUserMove(UserMoveEvent e) {
    User user = e.getUser();
    if (e.getTo().getY() < user.getWorld().getMinHeight() - 10) {
      user.getPlayer().setVelocity(new Vector());
      user.teleport(this.currentGame.getStartLocation());
    }
  }

  @EventHandler
  public void onUserDamage(UserDamageEvent e) {
    if (this.currentGame.isGameRunning()) {
      return;
    }

    e.setCancelDamage(true);
    e.setCancelled(true);
  }

  @EventHandler
  public void onEntityDamageByUser(EntityDamageByUserEvent e) {
    if (e.getUser().getStatus().equals(Status.User.OUT_GAME)) {
      e.setCancelDamage(true);
      e.setCancelled(true);
    }
  }

}
