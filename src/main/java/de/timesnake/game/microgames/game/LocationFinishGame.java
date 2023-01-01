/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;

public abstract class LocationFinishGame extends MicroGame implements Listener {

    protected static final Integer SPEC_LOCATION_INDEX = 0;
    protected static final Integer START_LOCATION_INDEX = 1;
    protected static final Integer SPAWN_LOCATION_INDEX = 2;
    protected static final Integer FINISH_LOCATION_INDEX = 3;

    public LocationFinishGame(String name, String displayName, Material material, String description,
                              Integer minPlayers) {
        super(name, displayName, material, description, minPlayers);
        Server.registerListener(this, GameMicroGames.getPlugin());
    }

    @Override
    public void load() {
        super.load();
        this.currentMap.getWorld().setPVP(false);
    }

    @Override
    protected void loadDelayed() {
        for (User user : Server.getPreGameUsers()) {
            user.teleport(this.getSpawnLocation());
            user.lockLocation(true);
        }
    }

    @Override
    public void start() {
        super.start();
        for (User user : Server.getInGameUsers()) {
            user.lockLocation(false);
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void reset() {
        super.reset();
        Server.getWorldManager().reloadWorld(this.currentMap.getWorld());
    }

    @Override
    public boolean onUserJoin(MicroGamesUser user) {
        return false;
    }

    @Override
    public void onUserQuit(MicroGamesUser user) {
        if (Server.getInGameUsers().size() == 0) {
            this.stop();
        }
    }

    @Override
    public Integer getLocationAmount() {
        return 4;
    }

    @Override
    public ExLocation getSpecLocation() {
        return super.currentMap.getLocation(SPEC_LOCATION_INDEX);
    }

    @Override
    public ExLocation getStartLocation() {
        return super.currentMap.getLocation(START_LOCATION_INDEX);
    }

    public ExLocation getSpawnLocation() {
        return this.currentMap.getLocation(SPAWN_LOCATION_INDEX);
    }

    public ExLocation getFinishLocation() {
        return this.currentMap.getLocation(FINISH_LOCATION_INDEX);
    }

    @EventHandler
    public void onDeath(UserDeathEvent e) {
        if (!this.isGameRunning()) {
            return;
        }
        this.onUserDeath(e);
    }

    protected abstract void onUserDeath(UserDeathEvent e);

    @EventHandler
    public void onMove(UserMoveEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        User user = e.getUser();
        if (!user.getStatus().equals(Status.User.IN_GAME)) {
            return;
        }

        if (user.getLocation().getY() < user.getWorld().getMinHeight()) {
            user.getPlayer().setVelocity(new Vector());
            user.teleport(this.getSpawnLocation());
            return;
        }

        this.onUserMove(e);
    }

    protected abstract void onUserMove(UserMoveEvent e);

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!this.isGameRunning()) {
            return;
        }
        e.setRespawnLocation(this.getSpawnLocation());
    }
}
