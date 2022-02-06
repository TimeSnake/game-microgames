package de.timesnake.game.microgames.server;

import com.google.common.collect.Lists;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.*;
import de.timesnake.basic.game.util.Game;
import de.timesnake.basic.game.util.GameServerManager;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.object.Status;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.game.*;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.game.microgames.user.PartyManager;
import de.timesnake.game.microgames.user.TablistManager;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class MicroGamesServerManager extends GameServerManager implements Listener {

    private static final Integer NEXT_GAME_DELAY = 10;

    public static MicroGamesServerManager getInstance() {
        return (MicroGamesServerManager) ServerManager.getInstance();
    }

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

        /*
        PhantomPunch phantomPunch = new PhantomPunch();
        if (phantomPunch.getMaps().size() > 0) {
            games.add(phantomPunch);
        }

         */

        TntRun tntRun = new TntRun();
        if (tntRun.getMaps().size() > 0) {
            games.add(tntRun);
        }

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

        /*
        BoatRace boatRace = new BoatRace();
        if (boatRace.getMaps().size() > 0) {
            games.add(boatRace);
        }

        */

        this.partyManager = new PartyManager();

        for (MicroGame game : games) {
            this.microGamesByName.put(game.getName(), game);
            List<MicroGame> list = this.microGamesByMinPlayers.computeIfAbsent(game.getMinPlayers(), k -> new ArrayList<>());
            list.add(game);
            Server.printText(Plugin.MICRO_GAMES, "Loaded game " + game.getDisplayName());
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
    protected Game loadGame(DbGame dbGame, boolean loadWorlds) {
        return super.loadGame(dbGame, true);
    }

    public void pause() {
        this.paused = true;
    }

    public void nextGame() {

        if (this.partyMode) {
            this.nextPartyGame();
            return;
        }

        MicroGame votedGame;

        // get voted games
        List<MicroGame> votedGames = new ArrayList<>();
        int voteSum = 0;
        for (MicroGame microGame : this.microGamesByName.values()) {
            if (microGame.getVotes() > 0 && Server.getGameNotServiceUsers().size() >= microGame.getMinPlayers()) {
                votedGames.add(microGame);
                voteSum += microGame.getVotes();
            }
        }

        if (voteSum == 0) {
            votedGame = this.getRandomGame();
        } else {
            int r = this.random.nextInt(voteSum);
            Iterator<MicroGame> iterator = votedGames.listIterator();
            do {
                votedGame = iterator.next();
                r -= votedGame.getVotes();
            } while (r >= 0 && iterator.hasNext());
        }

        if (votedGame == null) {
            this.broadcastMicroGamesMessage(ChatColor.WARNING + "No game found, waiting for more players");
            this.paused = true;
            return;
        }

        while (votedGame.getMaps().size() == 1 && this.currentGame.equals(votedGame)) {
            if (votedGames.size() > 1) {
                this.nextGame();
                return;
            } else {
                votedGame = this.getRandomGame();
            }
        }

        this.paused = false;

        this.startNextGame(votedGame);
    }

    private void startNextGame(MicroGame nextGame) {

        for (User user : Server.getInGameUsers()) {
            ((MicroGamesUser) user).joinSpectator();
        }

        this.broadcastMicroGamesMessage(ChatColor.WARNING + "Switching to " + ChatColor.VALUE + nextGame.getDisplayName());
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
                Server.printText(Plugin.MICRO_GAMES, "Paused game loop");
                return;
            }

            this.start = NEXT_GAME_DELAY;

            this.startTask = Server.runTaskTimerSynchrony(() -> {
                if (start == 0) {
                    if (Server.getPreGameUsers().size() == 0 || this.paused) {
                        this.paused = true;
                        Server.printText(Plugin.MICRO_GAMES, "Paused game loop");
                        this.startTask.cancel();
                        return;
                    }
                    Server.broadcastNote(Instrument.PLING, Note.natural(0, Note.Tone.A));
                    this.currentGame.start();
                    this.startTask.cancel();
                } else if (start <= 5) {
                    Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
                    MicroGamesServer.broadcastMicroGamesMessage(ChatColor.PUBLIC + "The game starts in " + ChatColor.VALUE + start + ChatColor.PUBLIC + " s");
                    Server.broadcastTitle("§c" + start, "", Duration.ofSeconds(1));
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

        this.broadcastMicroGamesMessage(ChatColor.WARNING + "Starting party mode!");
        this.broadcastMicroGamesMessage(Server.getChat().getLineSeparator());
        this.broadcastMicroGamesMessage(ChatColor.GOLD + "Points Distribution:");
        this.broadcastMicroGamesMessage(ChatColor.PUBLIC + "    1. Place: " + ChatColor.VALUE + MicroGame.FIRST_POINTS + " points");
        this.broadcastMicroGamesMessage(ChatColor.PUBLIC + "    2. Place: " + ChatColor.VALUE + MicroGame.SECOND_POINTS + " points");
        this.broadcastMicroGamesMessage(ChatColor.PUBLIC + "    3. Place: " + ChatColor.VALUE + MicroGame.THIRD_POINTS + " points");
        this.broadcastMicroGamesMessage(Server.getChat().getLineSeparator());
        Server.broadcastTitle("§cParty Mode", "Earn points by winning a game. The player with most points wins", Duration.ofSeconds(5));
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

        List<User> users = new LinkedList<>(Server.getGameNotServiceUsers());
        users.sort(Comparator.comparingInt((user) -> ((MicroGamesUser) user).getPoints()));
        users = Lists.reverse(users);

        this.broadcastMicroGamesMessage("");
        this.broadcastMicroGamesMessage("");
        this.broadcastMicroGamesMessage(ChatColor.WARNING + "The party has ended");
        this.broadcastMicroGamesMessage(Server.getChat().getLineSeparator());

        int i = 1;
        for (User user : users) {
            if (i == 1) {
                Server.broadcastTitle(user.getChatName() + " §fwins", "", Duration.ofSeconds(3));
            }
            this.broadcastMicroGamesMessage(ChatColor.GOLD + "§l " + i + ".  " + user.getChatName());
            i++;

            ((MicroGamesUser) user).resetPoints();
            this.getTablistManager().getTablist().updateEntryValue(user, ((MicroGamesUser) user).getPoints());
        }
        this.broadcastMicroGamesMessage(Server.getChat().getLineSeparator());

        this.partyGameIterator = null;
        this.partyMode = false;

        Server.runTaskLaterSynchrony(this::nextGame, 20 * 5, GameMicroGames.getPlugin());
    }

    public void broadcastMicroGamesMessage(String message) {
        Server.broadcastMessage(Plugin.MICRO_GAMES, message);
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
            Server.printText(Plugin.MICRO_GAMES, "Resumed game loop");
            this.nextGame();
        } else {
            if (!this.currentGame.onUserJoin(user)) {
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
            Server.printText(Plugin.MICRO_GAMES, "Paused game loop");
        }

        user.clearVotes();
    }

    @EventHandler
    public void onUserMove(UserMoveEvent e) {
        User user = e.getUser();
        if (!user.getStatus().equals(Status.User.IN_GAME) && e.getTo().getY() < -10) {
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
