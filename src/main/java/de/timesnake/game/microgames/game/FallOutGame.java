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

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;

public abstract class FallOutGame extends MicroGame implements Listener {

    public FallOutGame(String name, String displayName, Material material, String description, Integer minPlayers) {
        super(name, displayName, material, description, minPlayers);
    }

    public abstract Integer getDeathHeight();

    public abstract String getDeathMessage();

    @EventHandler
    public void onUserMove(UserMoveEvent e) {
        User user = e.getUser();

        if (!this.isGameRunning()) {
            return;
        }

        if (!user.getStatus().equals(Status.User.IN_GAME)) {
            return;
        }

        if (e.getTo().getBlockY() < this.getDeathHeight()) {
            user.showTitle(Component.text("You lose!", ExTextColor.WARNING), Component.empty(), Duration.ofSeconds(3));
            MicroGamesServer.broadcastMicroGamesMessage(user.getChatNameComponent()
                    .append(Component.text(this.getDeathMessage(), ExTextColor.WARNING)));
            Server.broadcastSound(Sound.ENTITY_PLAYER_HURT, 2);

            this.addWinner(((MicroGamesUser) user), false);
        }
    }

    @EventHandler
    public void onUserDamage(UserDamageEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        e.setCancelDamage(true);
    }
}
