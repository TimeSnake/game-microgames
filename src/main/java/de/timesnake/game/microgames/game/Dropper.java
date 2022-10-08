/*
 * game-microgames.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
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
