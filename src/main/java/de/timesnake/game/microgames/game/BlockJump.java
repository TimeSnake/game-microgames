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
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class BlockJump extends LocationFinishGame implements Listener {

    private static final Integer ARENA_SIZE = 50;
    private static final Material PLATFORM = Material.GREEN_CONCRETE;
    private static final double DENSITY = 0.37;
    private static final int HEIGHT = 50;

    private final Random random = new Random();

    public BlockJump() {
        super("blockjump", "BlockJump", Material.GREEN_TERRACOTTA, "Try to reach the top", 1);
    }

    @Override
    public void prepare() {
        super.prepare();
        this.generateLevel();
    }

    @Override
    public void start() {
        super.start();
        for (User user : Server.getInGameUsers()) {
            user.getPlayer().setWalkSpeed(0.3F);
            user.addPotionEffect(PotionEffectType.JUMP, 3);
        }
    }

    private void generateLevel() {
        ExWorld world = this.getSpawnLocation().getExWorld();
        int ground = this.getSpawnLocation().getBlockY();

        int xBorder = (int) (this.getSpecLocation().getBlockX() - 0.5 * ARENA_SIZE);
        int zBorder = (int) (this.getStartLocation().getBlockZ() - 0.5 * ARENA_SIZE);

        for (int y = 0; y < HEIGHT; y++) {
            for (int i = (int) (this.randomBM() * DENSITY * ARENA_SIZE); i > 0; i--) {
                int baseX = this.random.nextInt(ARENA_SIZE) + xBorder + y;
                int baseZ = this.random.nextInt(ARENA_SIZE) + zBorder;

                for (int x = 0; x <= 1; x++) {
                    for (int z = 0; z <= 1; z++) {
                        world.getBlockAt(baseX + x, ground + y, baseZ + z).setType(PLATFORM);
                    }
                }
            }


        }


    }

    private double randomBM() {
        double u = 0, v = 0;
        while (u == 0) u = Math.random(); //Converting [0,1) to (0,1)
        while (v == 0) v = Math.random();

        double num = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
        num = num / 10.0 + 0.5; // Translate to 0 -> 1
        if (num > 1 || num < 0) {
            this.randomBM(); // resample between 0 and 1
        }
        return num;
    }

    @Override
    public void stop() {
        super.stop();
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

    @Override
    protected void onUserMove(UserMoveEvent e) {
        if (e.getTo().getY() >= this.getSpawnLocation().getY() + HEIGHT) {
            super.addWinner(((MicroGamesUser) e.getUser()), true);
        }
    }

    @EventHandler
    public void onUserDamage(UserDamageEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        e.setCancelDamage(true);
        e.setCancelled(true);
    }

}
