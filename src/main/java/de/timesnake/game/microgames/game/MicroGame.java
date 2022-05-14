package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.Map;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.extension.util.chat.Chat;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public abstract class MicroGame {

    public static final int FIRST_POINTS = 3;
    public static final int SECOND_POINTS = 2;
    public static final int THIRD_POINTS = 1;

    protected final String name;
    protected final String displayName;
    protected final Material material;
    protected final String description;

    protected final Integer minPlayers;

    protected Map currentMap;
    protected Map previousMap;
    protected final List<Map> maps = new ArrayList<>();

    private boolean isGameRunning = false;

    protected final Random random = new Random();

    protected Sideboard sideboard;

    private Integer votes = 0;

    protected MicroGamesUser first;
    protected MicroGamesUser second;
    protected MicroGamesUser third;

    public MicroGame(String name, String displayName, Material material, String description, Integer minPlayers) {
        this.name = name;
        this.displayName = displayName;
        this.material = material;
        this.description = description;
        this.minPlayers = minPlayers;

        for (Map map : MicroGamesServer.getGame().getMaps()) {
            if (map.getInfo().get(0).equalsIgnoreCase(this.name)) {
                if (map.getWorld() == null) {
                    Server.printWarning(Plugin.MICRO_GAMES, "Can not load map " + map.getName() + ", world not exists");
                    continue;
                }

                if (map.getLocations().size() < this.getLocationAmount()) {
                    Server.printWarning(Plugin.MICRO_GAMES, "Can not load map " + map.getName() + ", too few locations");
                    continue;
                }

                this.maps.add(map);
                this.onMapLoad(map);

                Server.printText(Plugin.MICRO_GAMES, "Added map " + map.getName(), this.displayName);
            }
        }

        this.sideboard = Server.getScoreboardManager().registerNewSideboard(name, "§6§l" + displayName);
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
        world.allowFoodChange(false);
        world.allowFireSpread(false);
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
        for (User user : Server.getPreGameUsers()) {
            user.teleport(this.getStartLocation());
            if (this.sideboard != null) {
                user.setSideboard(this.sideboard);
            }
        }
        Server.broadcastTitle("§6§l" + this.displayName, this.description, Duration.ofSeconds(5));

        Server.runTaskLaterSynchrony(this::loadDelayed, 5 * 20, GameMicroGames.getPlugin());
    }

    protected abstract void loadDelayed();

    public void start() {
        for (User user : Server.getPreGameUsers()) {
            user.setStatus(Status.User.IN_GAME);
        }

        this.isGameRunning = true;
        MicroGamesServer.broadcastMicroGamesMessage("§cGame started");
    }

    protected void addWinner(MicroGamesUser user, boolean first) {
        if (!user.getStatus().equals(Status.User.OUT_GAME)) {
            user.joinSpectator();
        }

        int users = Server.getInGameUsers().size();

        if (user.equals(this.first) || user.equals(this.second) || user.equals(this.third)) {
            return;
        }

        if (first) {
            if (this.first == null) {
                this.first = user;
                MicroGamesServer.broadcastMicroGamesMessage(user.getChatName() + ChatColor.WARNING + " finished " + ChatColor.WARNING + "#1");
            } else if (this.second == null) {
                this.second = user;
                MicroGamesServer.broadcastMicroGamesMessage(user.getChatName() + ChatColor.WARNING + " finished " + ChatColor.WARNING + "#2");
            } else if (this.third == null) {
                this.third = user;
                MicroGamesServer.broadcastMicroGamesMessage(user.getChatName() + ChatColor.WARNING + " finished " + ChatColor.WARNING + "#3");
                this.stop();
                return;
            }
        } else {
            if (users == 2) {
                this.third = user;
            } else if (users == 1) {
                this.second = user;
                MicroGamesServer.broadcastMicroGamesMessage(user.getChatName() + ChatColor.WARNING + " finished " + ChatColor.WARNING + "#2");
                this.first = ((MicroGamesUser) Server.getInGameUsers().stream().filter((u) -> !u.equals(second)).iterator().next());
            } else if (users == 0) {
                this.first = user;
                MicroGamesServer.broadcastMicroGamesMessage(user.getChatName() + ChatColor.WARNING + " finished " + ChatColor.WARNING + "#1");
            }
        }

        if (users <= 1) {
            this.stop();
        }

    }

    protected void calcPlaces(Function<MicroGamesUser, Integer> usersToValue, boolean highest) {
        Tuple<MicroGamesUser, Integer> first = highest ? new Tuple<>(null, -1) : new Tuple<>(null, Integer.MAX_VALUE);
        Tuple<MicroGamesUser, Integer> second = highest ? new Tuple<>(null, -1) : new Tuple<>(null, Integer.MAX_VALUE);
        Tuple<MicroGamesUser, Integer> third = highest ? new Tuple<>(null, -1) : new Tuple<>(null, Integer.MAX_VALUE);

        for (User user : MicroGamesServer.getGameNotServiceUsers()) {
            Integer value = usersToValue.apply(((MicroGamesUser) user));

            if (value == null) {
                continue;
            }

            if ((highest && value > first.getB()) || (!highest && value < first.getB())) {
                second = first;
                first = new Tuple<>(((MicroGamesUser) user), value);
            } else if ((highest && value > second.getB()) || (!highest && value < second.getB())) {
                third = second;
                second = new Tuple<>(((MicroGamesUser) user), value);
            } else if ((highest && value > third.getB()) || (!highest && value < third.getB())) {
                third = new Tuple<>(((MicroGamesUser) user), value);
            }
        }

        this.first = first.getA();
        this.second = second.getA();
        this.third = third.getA();
    }

    public void stop() {
        this.isGameRunning = false;

        if (this.first != null) {
            MicroGamesServer.broadcastMicroGamesMessage(Chat.getLineSeparator());

            if (MicroGamesServer.isPartyMode()) {
                this.first.addPoints(FIRST_POINTS);
                MicroGamesServer.getTablistManager().getTablist().updateEntryValue(this.first, this.first.getPoints());
            }

            if (this.second == null) {
                MicroGamesServer.broadcastMicroGamesMessage(this.first.getChatName() + " §fwins!");
                Server.broadcastTitle(first.getChatName() + " §fwins", "", Duration.ofSeconds(3));
            } else {
                if (MicroGamesServer.isPartyMode()) {
                    this.second.addPoints(SECOND_POINTS);
                    MicroGamesServer.getTablistManager().getTablist().updateEntryValue(this.second, this.second.getPoints());
                }

                MicroGamesServer.broadcastMicroGamesMessage(ChatColor.GOLD + "§l1.  " + this.first.getChatName());
                MicroGamesServer.broadcastMicroGamesMessage(ChatColor.GOLD + "§l2.  " + this.second.getChatName());

                if (this.third == null) {
                    Server.broadcastTitle(first.getChatName() + " §fwins", "2. " + this.second.getChatName(), Duration.ofSeconds(3));
                } else {
                    if (MicroGamesServer.isPartyMode()) {
                        this.third.addPoints(THIRD_POINTS);
                        MicroGamesServer.getTablistManager().getTablist().updateEntryValue(this.third, this.third.getPoints());
                    }

                    MicroGamesServer.broadcastMicroGamesMessage(ChatColor.GOLD + "§l3.  " + this.third.getChatName());
                    Server.broadcastTitle(first.getChatName() + " §fwins", "2. " + this.second.getChatName() + "    3. " + this.third.getChatName(), Duration.ofSeconds(3));
                }
            }

            MicroGamesServer.broadcastMicroGamesMessage(Chat.getLineSeparator());
        }

        this.previousMap = this.currentMap;

        Server.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 2);

        MicroGamesServer.nextGame();
    }

    public void reset() {
        this.first = null;
        this.second = null;
        this.third = null;
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
