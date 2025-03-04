/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.CancelPriority;
import de.timesnake.basic.bukkit.util.user.event.UserBlockPlaceEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.game.basis.LocationFinishGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoatRace extends LocationFinishGame implements Listener {

  private final Set<User> enterableUsers = new HashSet<>();

  private final HashMap<User, Boat> boatByUser = new HashMap<>();

  public BoatRace() {
    super("boat_race",
        "Boat Race",
        Material.OAK_BOAT,
        "Try to be first at finish",
        List.of("§hGoal: §pfirst at finish", "Steer the boat to the finish (beacon)."),
        1,
        Duration.ofMinutes(3));
  }

  @Override
  public void applyBeforeStart() {
    super.applyBeforeStart();

    for (User user : Server.getPreGameUsers()) {
      this.setUserInBoat(user);
    }
  }

  @Override
  public void start() {
    super.start();

    for (User user : Server.getInGameUsers()) {
      user.setAllowFlight(false);
    }

    this.getStartLocation().clone().add(0, -1, 0).getBlock().setType(Material.WATER);
  }

  @Override
  public void stop() {
    this.boatByUser.forEach((u, b) -> b.remove());
    this.boatByUser.clear();
    super.stop();
  }

  @Override
  public void reset() {
    super.reset();

    if (this.previousMap != null) {
      for (Boat boat : this.previousMap.getWorld().getEntitiesByClass(Boat.class)) {
        boat.remove();
      }
    }
  }

  @Override
  public boolean hasSideboard() {
    return false;
  }

  @Override
  protected void onUserMove(UserMoveEvent e) {
    MicroGamesUser user = (MicroGamesUser) e.getUser();
    if (this.getFinishLocation().distance(e.getTo()) < 3) {
      super.addWinner(user, true);
      Boat boat = this.boatByUser.get(user);
      if (boat != null) {
        boat.remove();
      }
    }
  }

  private void setUserInBoat(User user) {
    Boat boat = (Boat) this.getStartLocation().getWorld().spawnEntity(this.getStartLocation(), EntityType.BOAT);
    boat.setInvulnerable(true);
    boat.setRotation(this.getStartLocation().getYaw(), 0);

    Boat oldBoat = this.boatByUser.get(user);

    if (oldBoat != null) {
      oldBoat.eject();
      oldBoat.remove();
    }

    this.enterableUsers.add(user);
    boat.addPassenger(user.getPlayer());

    user.setAllowFlight(true);

    this.boatByUser.put(user, boat);
  }

  @EventHandler
  public void onVehicleMove(VehicleMoveEvent e) {
    if (this.isGameRunning()) {
      return;
    }

    if (this.currentMap == null || !this.currentMap.getWorld().getBukkitWorld().equals(e.getFrom().getWorld())) {
      return;
    }

    if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
      return;
    }

    if (!e.getVehicle().getType().equals(EntityType.BOAT)) {
      return;
    }

    Entity entity = e.getVehicle().getPassengers().get(0);

    if (!(entity instanceof Player player)) {
      return;
    }

    User user = Server.getUser(player);

    if (user == null || user.isService()) {
      return;
    }

    this.setUserInBoat(user);
  }

  @EventHandler
  public void onVehicleEnter(VehicleEnterEvent e) {
    if (this.currentMap == null || !(e.getEntered() instanceof Player)) {
      return;
    }

    if (!e.getEntered().getWorld().equals(this.currentMap.getWorld().getBukkitWorld())) {
      return;
    }

    User user = Server.getUser((Player) e.getEntered());

    if (user.isService()) {
      return;
    }

    if (this.enterableUsers.contains(user)) {
      this.enterableUsers.remove(user);
      return;
    }

    e.setCancelled(true);
  }

  @EventHandler
  public void onVehicleExit(VehicleExitEvent e) {
    if (this.currentMap == null) {
      return;
    }

    if (!(e.getExited() instanceof Player player)) {
      return;
    }

    User user = Server.getUser(player);

    if (user == null || user.isService()) {
      return;
    }

    if (!e.getExited().getWorld().equals(this.currentMap.getWorld().getBukkitWorld())) {
      return;
    }

    e.setCancelled(true);
  }

  @EventHandler
  public void onUserDamage(UserDamageEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (e.getUser().isService()) {
      return;
    }

    e.setCancelDamage(true);
  }

  @EventHandler
  public void onBlockPlace(UserBlockPlaceEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    User user = e.getUser();

    if (user.isService()) {
      return;
    }

    if (!e.getBlockPlaced().getType().equals(Material.LILY_PAD)) {
      return;
    }

    e.setCancelled(CancelPriority.HIGH, false);
  }
}
