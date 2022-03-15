package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.chat.Argument;
import de.timesnake.basic.bukkit.util.chat.CommandListener;
import de.timesnake.basic.bukkit.util.chat.Sender;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.List;

public class SkipGameCmd implements CommandListener {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!args.isLengthEquals(1, true)) {
            return;
        }

        if (args.get(0).equalsIgnoreCase("skip")) {
            if (!sender.hasPermission("microgames.skip", 2506)) {
                return;
            }

            if (MicroGamesServer.getCurrentGame() != null && MicroGamesServer.getCurrentGame().isGameRunning()) {
                MicroGamesServer.skipGame();
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return List.of("skip");
    }
}
