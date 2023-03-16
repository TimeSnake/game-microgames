/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

public class LadderKing extends MicroGame implements Listener {

    private static final Integer START_LOCATION_INDEX = 0;
    private static final Integer SPEC_LOCATION_INDEX = 1;
    private static final Integer LADDER_LOCATION_INDEX = 2;
    // spawn locations all above 2

    private static final Integer TIME = 90;

    private Integer time = TIME;

    private BukkitTask task;

    private final HashMap<MicroGamesUser, Integer> ladderTimesByUser = new HashMap<>();

    public LadderKing() {
        super("ladderking", "King of the Ladder", Material.LADDER,
                "Try stand the longest time on top of the ladder",
                2, -1);
        Server.registerListener(this, GameMicroGames.getPlugin());
    }

    @Override
    public Integer getLocationAmount() {
        return 3;
    }

    @Override
    public boolean hasSideboard() {
        return true;
    }

    @Override
    public void prepare() {
        super.prepare();
    }

    @Override
    public void load() {
        super.load();

        super.sideboard.setScore(4, "§9§lTime");
        super.sideboard.setScore(3, this.getTimeString());
        super.sideboard.setScore(2, "§f---------------");
        super.sideboard.setScore(1, "§c§lScore");
        super.sideboard.setScore(0, "0");
    }

    @Override
    protected void loadDelayed() {
        List<ExLocation> spawnLocations = super.currentMap.getLocations(3);
        for (User user : Server.getPreGameUsers()) {
            user.teleport(spawnLocations.get(super.random.nextInt(spawnLocations.size())));
            user.lockLocation(true);
        }
    }

    private String getTimeString() {
        if (this.time >= 60) {
            return this.time / 60 + "min " + this.time % 60 + "s";
        } else {
            return this.time % 60 + "s";
        }
    }

    @Override
    public void start() {
        super.start();

        for (User user : MicroGamesServer.getInGameUsers()) {
            this.ladderTimesByUser.put(((MicroGamesUser) user), 0);
            user.lockLocation(false);
        }

        this.task = Server.runTaskTimerSynchrony(() -> {
            this.time--;
            super.sideboard.setScore(3, this.getTimeString());

            for (User user : Server.getInGameUsers()) {
                if (this.isOnTop(user)) {
                    Integer ladderTime = this.ladderTimesByUser.get(((MicroGamesUser) user));
                    if (ladderTime != null) {
                        ladderTime++;
                        this.ladderTimesByUser.put(((MicroGamesUser) user), ladderTime);
                        user.setSideboardScore(0, "" + ladderTime);
                    }
                }
            }

            if (this.time == 0) {
                this.stop();
            }
        }, 0, 20, GameMicroGames.getPlugin());
    }

    private boolean isOnTop(User user) {
        Block userBlock = user.getLocation().getBlock();
        Block topBlock = super.currentMap.getLocation(LADDER_LOCATION_INDEX).getBlock();
        if (userBlock.getY() >= topBlock.getY()) {
            return userBlock.getLocation().distanceSquared(topBlock.getLocation()) <= 1;
        }
        return false;
    }

    @Override
    public void stop() {
        super.calcPlaces(this.ladderTimesByUser::get, true);

        if (this.task != null) {
            this.task.cancel();
        }

        super.stop();
    }

    @Override
    public void reset() {
        super.reset();
        this.time = TIME;
        super.sideboard.setScore(3, this.getTimeString());
        this.ladderTimesByUser.clear();
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
        return super.currentMap.getLocation(SPEC_LOCATION_INDEX);
    }

    @Override
    public ExLocation getStartLocation() {
        return super.currentMap.getLocation(START_LOCATION_INDEX);
    }

    @EventHandler
    public void onUserDamage(UserDamageEvent e) {
        if (!this.isGameRunning()) {
            return;
        }
        e.setCancelDamage(true);
    }

    @EventHandler
    public void onUserDamageByUser(UserDamageByUserEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        if (!e.getUserDamager().getStatus().equals(Status.User.IN_GAME)) {
            e.setCancelDamage(true);
            e.setCancelled(true);
        }
    }

}
