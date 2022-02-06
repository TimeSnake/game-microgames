package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.ExItemStack;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserInventoryInteractEvent;
import de.timesnake.basic.bukkit.util.user.event.UserInventoryInteractListener;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.server.MicroGamesServer;
import org.bukkit.Material;

public class PartyManager implements UserInventoryInteractListener {

    public static final ExItemStack ITEM = new ExItemStack(Material.PLAYER_HEAD, "§6Party");

    public PartyManager() {
        Server.getInventoryEventManager().addInteractListener(this, ITEM);
    }

    @Override
    public void onUserInventoryInteract(UserInventoryInteractEvent event) {
        User user = event.getUser();

        if (user.hasPermission("microgames.party", 2505, Plugin.MICRO_GAMES)) {
            if (!MicroGamesServer.getCurrentGame().isGameRunning() && !MicroGamesServer.isPartyMode()) {
                user.sendPluginMessage(Plugin.MICRO_GAMES, "Initiating party mode");
                MicroGamesServer.startParty();
            } else {
                user.sendPluginMessage(Plugin.MICRO_GAMES, "Party Mode currently not available");
            }
        }
    }
}
