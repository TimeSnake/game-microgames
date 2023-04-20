/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.server;

import com.google.common.collect.Lists;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.EntityDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserJoinEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.user.event.UserQuitEvent;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.user.scoreboard.Tablist;
import de.timesnake.basic.game.util.game.Game;
import de.timesnake.basic.game.util.server.GameServerManager;
import de.timesnake.basic.game.util.user.SpectatorManager;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.game.BlockJump;
import de.timesnake.game.microgames.game.BoatRace;
import de.timesnake.game.microgames.game.ColorSwap;
import de.timesnake.game.microgames.game.Dropper;
import de.timesnake.game.microgames.game.Firefighter;
import de.timesnake.game.microgames.game.HotPotato;
import de.timesnake.game.microgames.game.KnockOut;
import de.timesnake.game.microgames.game.LadderKing;
import de.timesnake.game.microgames.game.Parkour;
import de.timesnake.game.microgames.game.Spleef;
import de.timesnake.game.microgames.game.TntRun;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.game.microgames.user.PartyManager;
import de.timesnake.game.microgames.user.TablistManager;
import de.timesnake.library.basic.util.Loggers;
import de.timesnake.library.basic.util.RandomCollection;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.game.NonTmpGameInfo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

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

        List<MicroGame> games = new ArrayList<>();

        LadderKing ladderKing = new LadderKing();
        if (ladderKing.getMaps().size() > 0) {
            games.add(ladderKing);
        }

        ColorSwap colorSwap = new ColorSwap();
        if (colorSwap.getMaps().size() > 0) {
            games.add(colorSwap);
        }

        //PhantomPunch phantomPunch = new PhantomPunch();
        //if (phantomPunch.getMaps().size() > 0) {
        //    games.add(phantomPunch);
        //}

        Parkour parkour = new Parkour();
        if (parkour.getMaps().size() > 0) {
            games.add(parkour);
        }

        KnockOut knockOut = new KnockOut();
        if (knockOut.getMaps().size() > 0) {
            games.add(knockOut);
        }

        BlockJump blockJump = new BlockJump();
        if (blockJump.getMaps().size() > 0) {
            games.add(blockJump);
        }

        HotPotato hotPotato = new HotPotato();
        if (hotPotato.getMaps().size() > 0) {
            games.add(hotPotato);
        }

        Dropper dropper = new Dropper();
        if (dropper.getMaps().size() > 0) {
            games.add(dropper);
        }

        BoatRace boatRace = new BoatRace();
        if (boatRace.getMaps().size() > 0) {
            games.add(boatRace);
        }

        TntRun tntRun = new TntRun();
        if (tntRun.getMaps().size() > 0) {
            games.add(tntRun);
        }

        Firefighter firefighter = new Firefighter();
        if (firefighter.getMaps().size() > 0) {
            games.add(firefighter);
        }

        //Graffiti graffiti = new Graffiti();
        //if (graffiti.getMaps().size() > 0) {
        //    games.add(graffiti);
        //}

        Spleef spleef = new Spleef();
        if (spleef.getMaps().size() > 0) {
            games.add(spleef);
        }

        this.partyManager = new PartyManager();

        for (MicroGame game : games) {
            this.microGamesByName.put(game.getName(), game);
            List<MicroGame> list = this.microGamesByMinPlayers.computeIfAbsent(game.getMinPlayers(),
                    k -> new ArrayList<>());
            list.add(game);
            Loggers.GAME.info("Loaded game " + game.getDisplayName());
        }

        this.tablistManager = new TablistManager();

        Server.registerListener(this, GameMicroGames.getPlugin());

        this.currentGame = games.get(this.random.nextInt(games.size()));
        this.currentGame.prepare();
        this.currentGame.load();
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

        RandomCollection<MicroGame> votedGames = new RandomCollection<>(this.random);

        votedGames.addAll(this.microGamesByName.values().stream()
                        .filter(mg -> Server.getGameNotServiceUsers().size() >= mg.getMinPlayers()
                                && (mg.getMaps().size() > 1 || !mg.equals(this.currentGame)))
                        .toList(),
                mg -> mg.getVotes().doubleValue());

        if (votedGames.size() == 0) {
            votedGame = this.getRandomGame();
        } else {
            votedGame = votedGames.next();
        }

        if (votedGame == null) {
            this.broadcastMicroGamesMessage(
                    Component.text("No game found, waiting for more players", ExTextColor.WARNING));
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

        this.broadcastMicroGamesMessage(Component.text("Switching to ", ExTextColor.WARNING)
                .append(Component.text(nextGame.getDisplayName(), ExTextColor.VALUE)));
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
                    MicroGamesServer.broadcastMicroGamesMessage(
                            Component.text("The game starts in ", ExTextColor.PUBLIC)
                                    .append(Component.text(start, ExTextColor.VALUE))
                                    .append(Component.text(" s", ExTextColor.PUBLIC)));
                    Server.broadcastTitle(Component.text(start, ExTextColor.WARNING),
                            Component.empty(), Duration.ofSeconds(1));
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

        this.broadcastMicroGamesMessage(
                Component.text("Starting party mode!", ExTextColor.WARNING));
        this.broadcastMicroGamesMessage(Chat.getLineSeparator());
        this.broadcastMicroGamesMessage(Component.text("Point Distribution:", ExTextColor.GOLD));
        this.broadcastMicroGamesMessage(Component.text("    1. Place: ", ExTextColor.PUBLIC)
                .append(Component.text(MicroGame.FIRST_POINTS + " points", ExTextColor.VALUE)));
        this.broadcastMicroGamesMessage(Component.text("    2. Place: ", ExTextColor.PUBLIC)
                .append(Component.text(MicroGame.SECOND_POINTS + " points", ExTextColor.VALUE)));
        this.broadcastMicroGamesMessage(Component.text("    3. Place: ", ExTextColor.PUBLIC)
                .append(Component.text(MicroGame.THIRD_POINTS + " points", ExTextColor.VALUE)));
        this.broadcastMicroGamesMessage(Chat.getLineSeparator());
        Server.broadcastTitle(Component.text("Party Mode", ExTextColor.WARNING),
                Component.text("Earn points by winning a game. The player with most points wins"),
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

            this.broadcastMicroGamesMessage(Component.empty());
            this.broadcastMicroGamesMessage(Component.empty());
            this.broadcastMicroGamesMessage(
                    Component.text("The party has ended", ExTextColor.WARNING));
            this.broadcastMicroGamesMessage(Chat.getLineSeparator());

            Server.broadcastTitle(users.get(0).getChatNameComponent()
                            .append(Component.text(" wins", ExTextColor.WHITE)),
                    Component.text("The party has ended", ExTextColor.WARNING),
                    Duration.ofSeconds(4));

            int i = 1;
            for (User user : users) {
                this.broadcastMicroGamesMessage(
                        Component.text(" " + i + ".  ", ExTextColor.GOLD, TextDecoration.BOLD)
                                .append(user.getChatNameComponent()));
                i++;

                ((MicroGamesUser) user).resetPoints();
                this.getTablistManager().getTablist()
                        .updateEntryValue(user, ((MicroGamesUser) user).getPoints());
            }
            this.broadcastMicroGamesMessage(Chat.getLineSeparator());

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
