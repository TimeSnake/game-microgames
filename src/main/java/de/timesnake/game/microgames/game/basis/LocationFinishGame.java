/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.user.event.UserRespawnEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.List;

public abstract class LocationFinishGame extends MicroGame implements Listener {

  protected static final Integer FINISH_LOCATION_INDEX = 3;


  public LocationFinishGame(String name, String displayName, Material material, String headLine,
                            List<String> description,
                            Integer minPlayers, Duration maxTime) {
    super(name, displayName, material, headLine, description, minPlayers, maxTime);
  }

  @Override
  public void load() {
    super.load();
    this.currentMap.getWorld().setPVP(false);
  }

  @Override
  public void applyBeforeStart() {
    super.applyBeforeStart();

    for (User user : Server.getPreGameUsers()) {
      user.lockLocation();
    }
  }

  @Override
  public void start() {
    super.start();
    for (User user : Server.getInGameUsers()) {
      user.unlockLocation();
    }
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  public void onUserQuit(MicroGamesUser user) {
    if (Server.getInGameUsers().isEmpty()) {
      this.stop();
    }
  }

  public ExLocation getFinishLocation() {
    return this.currentMap.getLocation(FINISH_LOCATION_INDEX);
  }

  @EventHandler
  public void onDeath(UserDeathEvent e) {
    if (!this.isGameRunning()) {
      return;
    }
    e.setAutoRespawn(true);
    this.onUserDeath(e);
  }

  protected void onUserDeath(UserDeathEvent e) {
    e.setBroadcastDeathMessage(false);
  }

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
      this.onUserFallIntoVoid(user);
      return;
    }

    this.onUserMove(e);
  }

  protected void onUserFallIntoVoid(User user) {
    user.teleport(this.getSpawnLocation());
  }

  protected abstract void onUserMove(UserMoveEvent e);

  @EventHandler
  public void onRespawn(UserRespawnEvent e) {
    if (!this.isGameRunning()) {
      return;
    }
    this.onUserRepsawn(e);
  }

  protected void onUserRepsawn(UserRespawnEvent e) {
    e.setRespawnLocation(this.getSpawnLocation());
  }
}
