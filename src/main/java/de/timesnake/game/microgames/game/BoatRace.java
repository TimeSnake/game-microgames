package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.event.entity.EntityDismountEvent;

public class BoatRace extends LocationFinishGame implements Listener {


    public BoatRace() {
        super("boatrace", "Boat Race", Material.OAK_BOAT, "Try to be first at the finish", 1);
    }

    @Override
    protected void loadDelayed() {
        for (User user : Server.getPreGameUsers()) {
            user.teleport(this.getSpawnLocation());
            user.lockLocation(true);
            Boat boat = (Boat) this.getSpawnLocation().getWorld().spawnEntity(this.getSpawnLocation(), EntityType.BOAT);
            boat.setInvulnerable(true);
            boat.addPassenger(user.getPlayer());
        }
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public boolean hasSideboard() {
        return false;
    }

    @Override
    protected void onUserDeath(UserDeathEvent e) {
        e.setAutoRespawn(true);
        e.setBroadcastDeathMessage(false);
    }

    @Override
    public void onRespawn(PlayerRespawnEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        e.setRespawnLocation(this.getSpawnLocation());
        Player p = e.getPlayer();
        Server.runTaskLaterSynchrony(() -> {
            Boat boat = (Boat) this.getSpawnLocation().getWorld().spawnEntity(this.getSpawnLocation(), EntityType.BOAT);
            boat.setPersistent(true);
            boat.setInvulnerable(true);
            boat.addPassenger(p);
        }, 10, GameMicroGames.getPlugin());

    }

    @Override
    protected void onUserMove(UserMoveEvent e) {
        MicroGamesUser user = (MicroGamesUser) e.getUser();
        if (this.getFinishLocation().distance(e.getTo()) < 2) {
            super.first = user;
            this.stop();
        }
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        e.setCancelled(true);
    }
}
