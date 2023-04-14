/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.FallOutGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TntRun extends FallOutGame implements Listener {

    protected static final Integer SPEC_LOCATION_INDEX = 0;
    protected static final Integer START_LOCATION_INDEX = 1;
    protected static final Integer SPAWN_LOCATION_INDEX = 2;
    protected static final Integer DEATH_HEIGHT_LOCATION_INDEX = 3;

    protected static final Integer REMOVE_DELAY = 19;
    protected static final Integer TNT_REMOVE_DELAY = 10;

    protected static final Double[][] NEAR_BLOCK_VECTORS = {{0.3, 0.0}, {0.0, 0.3}, {-0.3, 0.0},
            {0.0, -0.3}, {0.3, 0.3}, {0.3, -0.3}, {-0.3, 0.3}, {-0.3, -0.3}};

    private final Set<Block> removedBlocks = new HashSet<>();

    public TntRun() {
        super("tntrun", "TNT Run", Material.TNT, "Try not to fall",
                1, 300);

        Server.registerListener(this, GameMicroGames.getPlugin());
    }

    @Override
    public Integer getLocationAmount() {
        return 4;
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
        }
    }

    @Override
    public void start() {
        super.start();

        for (User user : Server.getInGameUsers()) {
            user.getPlayer().setInvulnerable(true);

            Location from = user.getLocation();

            Server.runTaskLaterSynchrony(() -> {
                        this.removeBlocks(from.clone().add(0, -1, 0));
                        this.removeBlocks(from.clone().add(0, -2, 0));
                    }, 40 - REMOVE_DELAY,
                    GameMicroGames.getPlugin());
        }
    }

    private void removeBlocks(Location from) {

        Set<Block> blocks = new HashSet<>();

        if (this.isBlockRemoveable(from.getBlock())) {
            from.getBlock().setType(Material.TNT);
            blocks.add(from.getBlock());
            this.removedBlocks.add(from.getBlock());
        }

        for (Double[] vec : NEAR_BLOCK_VECTORS) {
            Location loc = from.clone().add(vec[0], 0, vec[1]);
            if (this.isBlockRemoveable(loc.getBlock())) {
                loc.getBlock().setType(Material.TNT);
                this.removedBlocks.add(loc.getBlock());
                blocks.add(loc.getBlock());
            }
        }

        Server.runTaskLaterSynchrony(() -> {
            for (Block block : blocks) {
                Location loc = block.getLocation().add(0.5, 0, 0.5);
                loc.getBlock().setType(Material.AIR);
                TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
                Server.runTaskLaterSynchrony(tnt::remove, TNT_REMOVE_DELAY,
                        GameMicroGames.getPlugin());
            }
        }, REMOVE_DELAY, GameMicroGames.getPlugin());

    }

    private boolean isBlockRemoveable(Block block) {
        return !block.getType().equals(Material.AIR)
                && !block.getType().equals(Material.TNT)
                && !this.removedBlocks.contains(block);
    }

    @Override
    public void reset() {
        super.reset();
        this.removedBlocks.clear();
        if (this.previousMap != null) {
            Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
        }
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

    public ExLocation getDeathLocation() {
        return super.currentMap.getLocation(DEATH_HEIGHT_LOCATION_INDEX);
    }

    @Override
    public Integer getDeathHeight() {
        return this.getDeathLocation().getBlockY();
    }

    @EventHandler
    @Override
    public void onUserMove(UserMoveEvent e) {
        User user = e.getUser();

        if (!this.isGameRunning()) {
            return;
        }

        if (!user.getStatus().equals(Status.User.IN_GAME)) {
            return;
        }

        super.onUserMove(e);

        e.getFrom().getBlock().setType(Material.AIR);
        this.removeBlocks(e.getFrom().clone().add(0, -1, 0));
        this.removeBlocks(e.getFrom().clone().add(0, -2, 0));
    }

}
