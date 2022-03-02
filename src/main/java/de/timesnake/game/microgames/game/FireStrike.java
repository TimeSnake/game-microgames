package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;

public class FireStrike extends MicroGame {

    public FireStrike() {
        super("firestrike", "Fire Strike", Material.BLAZE_POWDER, "Knock the fire out", 1);
    }

    @Override
    public Integer getLocationAmount() {
        return null;
    }

    @Override
    protected void loadDelayed() {

    }

    @Override
    public boolean hasSideboard() {
        return false;
    }

    @Override
    public boolean onUserJoin(MicroGamesUser user) {
        return false;
    }

    @Override
    public void onUserQuit(MicroGamesUser user) {

    }

    @Override
    public ExLocation getSpecLocation() {
        return null;
    }

    @Override
    public ExLocation getStartLocation() {
        return null;
    }
}
