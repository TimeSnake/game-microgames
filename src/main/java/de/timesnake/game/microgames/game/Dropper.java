/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Dropper extends LocationFinishGame implements Listener {

    public Dropper() {
        super("dropper", "Dropper", Material.ANVIL, "Try to reach the ground without hitting any block", 1);
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

    @EventHandler
    public void onUserDamage(UserDamageEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        e.getUser().teleport(this.getSpawnLocation());
        e.setCancelDamage(true);
    }

    @Override
    public void onUserMove(UserMoveEvent e) {
        if (e.getUser().getLocation().getY() <= this.getFinishLocation().getY()) {
            super.addWinner(((MicroGamesUser) e.getUser()), true);
        }
    }
}
