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
