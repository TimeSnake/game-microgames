/*
 * Copyright (C) 2023 timesnake
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
import java.time.Duration;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public abstract class FallOutGame extends MicroGame implements Listener {

    public FallOutGame(String name, String displayName, Material material, String description,
            Integer minPlayers) {
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
            user.showTitle(Component.text("You lose!", ExTextColor.WARNING), Component.empty(),
                    Duration.ofSeconds(3));
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
