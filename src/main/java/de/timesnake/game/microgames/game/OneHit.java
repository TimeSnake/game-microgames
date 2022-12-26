/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;

public class OneHit extends MicroGame {


    public OneHit(String name, String displayName, Material material, String description, Integer minPlayers) {
        super(name, displayName, material, description, minPlayers);
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
