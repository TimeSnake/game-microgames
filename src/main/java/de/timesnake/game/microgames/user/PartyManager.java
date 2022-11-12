/*
 * workspace.game-microgames.main
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

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.ExItemStack;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserInventoryInteractEvent;
import de.timesnake.basic.bukkit.util.user.event.UserInventoryInteractListener;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class PartyManager implements UserInventoryInteractListener {

    public static final ExItemStack ITEM = new ExItemStack(Material.PLAYER_HEAD, "§6Party");

    private Code.Permission perm;

    public PartyManager() {
        Server.getInventoryEventManager().addInteractListener(this, ITEM);

        this.perm = Plugin.MICRO_GAMES.createPermssionCode("mgp", "microgames.party");
    }

    @Override
    public void onUserInventoryInteract(UserInventoryInteractEvent event) {
        User user = event.getUser();

        if (!user.isSneaking()) {
            user.sendPluginMessage(Plugin.MICRO_GAMES, Component.text("Click while sneaking to activate", ExTextColor.PERSONAL));
            return;
        }

        if (user.hasPermission(this.perm, Plugin.MICRO_GAMES)) {
            if (!MicroGamesServer.getCurrentGame().isGameRunning() && !MicroGamesServer.isPartyMode()) {
                user.sendPluginMessage(Plugin.MICRO_GAMES, Component.text("Initiating party mode", ExTextColor.PERSONAL));
                MicroGamesServer.startParty();
            } else {
                user.sendPluginMessage(Plugin.MICRO_GAMES, Component.text("Party Mode currently not available", ExTextColor.PERSONAL));
            }
        }
    }
}
