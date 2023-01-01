/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.event.Listener;

public class Parkour extends LocationFinishGame implements Listener {

    public Parkour() {
        super("parkour", "Parkour", Material.GOLDEN_BOOTS,
                "Beat the parkour as fast you can", 1);
    }

    @Override
    public boolean hasSideboard() {
        return false;
    }

    @Override
    protected void onUserDeath(UserDeathEvent e) {
        e.setAutoRespawn(true);
    }

    @Override
    public void onUserMove(UserMoveEvent e) {
        if (e.getUser().getLocation().getBlock().equals(this.getFinishLocation().getBlock())) {
            super.addWinner(((MicroGamesUser) e.getUser()), true);
        }
    }

}
