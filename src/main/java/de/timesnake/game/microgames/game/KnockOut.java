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
import de.timesnake.basic.bukkit.util.user.ExItemStack;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class KnockOut extends ShrinkingPlatformGame implements Listener {

    public static final Integer START_RADIUS = 10;
    public static final Integer MIN_RADIUS = 3;
    public static final Integer DECREASE_DELAY = 20;
    protected static final Integer SPEC_LOCATION_INDEX = 0;
    protected static final Integer START_LOCATION_INDEX = 1;
    protected static final Integer SPAWN_LOCATION_INDEX = 2;
    protected static final ExItemStack STICK = new ExItemStack(Material.STICK).addExEnchantment(Enchantment.KNOCKBACK, 2);


    public KnockOut() {
        super("knockout", "KnockOut", Material.STICK, "Knock all players from the platform", 2);
        Server.registerListener(this, GameMicroGames.getPlugin());
    }

    @Override
    public Integer getLocationAmount() {
        return 3;
    }

    @Override
    public void prepare() {
        super.prepare();
        super.currentMap.getWorld().setPVP(false);
    }

    @Override
    protected void loadDelayed() {
        for (User user : Server.getPreGameUsers()) {
            user.teleport(this.getSpawnLocation());
            user.lockInventoryItemMove();
            user.setItem(0, STICK);
        }
    }

    @Override
    public void start() {
        super.start();
        super.currentMap.getWorld().setPVP(true);
    }

    @Override
    public Integer getStartRadius() {
        return START_RADIUS;
    }

    @Override
    public Integer getDelay() {
        return DECREASE_DELAY;
    }

    @Override
    public Integer getMinRadius() {
        return MIN_RADIUS;
    }

    @Override
    public ExLocation getCenterLocation() {
        return this.getSpawnLocation();
    }

    @Override
    public void reset() {
        super.reset();
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
        if (Server.getInGameUsers().size() <= 1) {
            this.stop();
        }
    }

    @Override
    public ExLocation getSpecLocation() {
        return super.currentMap.getLocation(SPEC_LOCATION_INDEX);
    }

    @Override
    public ExLocation getStartLocation() {
        return super.currentMap.getLocation(START_LOCATION_INDEX);
    }

    public ExLocation getSpawnLocation() {
        return this.currentMap.getLocation(SPAWN_LOCATION_INDEX);
    }

    @EventHandler
    public void onEntityDamage(UserDamageEvent e) {
        if (!this.isGameRunning()) {
            return;
        }
        e.setCancelDamage(true);
    }

    @Override
    public Integer getDeathHeight() {
        return this.getSpawnLocation().getBlockY();
    }

    @Override
    public String getDeathMessage() {
        return " was punched out!";
    }
}
