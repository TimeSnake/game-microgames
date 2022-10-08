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
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.library.basic.util.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

public abstract class ShrinkingPlatformGame extends FallOutGame {

    private BukkitTask decreaseTask;
    private Integer delay;
    private Integer radius;

    public ShrinkingPlatformGame(String name, String displayName, Material material, String description,
                                 Integer minPlayers) {
        super(name, displayName, material, description, minPlayers);
    }

    @Override
    public void prepare() {
        super.prepare();
        this.radius = this.getStartRadius();
        this.setPlatform();
    }

    @Override
    public void start() {
        super.start();

        this.delay = this.getDelay();

        this.decreaseTask = Server.runTaskTimerSynchrony(() -> {
            this.delay--;

            if (delay <= 3) {
                Server.broadcastNote(Instrument.PLING, Note.natural(0, Note.Tone.C));
                if (delay == 3) {
                    Server.broadcastTitle(Component.text("!", ExTextColor.WARNING),
                            Component.text("The platform becomes smaller"), Duration.ofSeconds(3));
                }
            }

            if (this.delay == 0) {
                this.radius--;
                if (this.radius < 0) {
                    this.decreaseTask.cancel();
                }
                this.delay = this.getDelay();
                this.setPlatform();
            }

        }, 0, 20, GameMicroGames.getPlugin());
    }

    @Override
    public void stop() {
        if (this.decreaseTask != null) {
            this.decreaseTask.cancel();
        }

        super.stop();
    }

    public abstract Integer getStartRadius();

    public abstract Integer getDelay();

    public abstract Integer getMinRadius();

    public abstract ExLocation getCenterLocation();

    private void setPlatform() {

        Location loc = this.getCenterLocation().clone().add(0, -1, 0);

        int maxRadiusSquared = this.getStartRadius() * this.getStartRadius();
        int radiusSquared = this.radius * this.radius;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distSquared = (x * x) + (z * z);
                if (distSquared <= maxRadiusSquared) {
                    if (distSquared <= radiusSquared) {
                        loc.clone().add(x, 0, z).getBlock().setType(Material.SHROOMLIGHT);
                    } else {
                        loc.clone().add(x, 0, z).getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }
}
