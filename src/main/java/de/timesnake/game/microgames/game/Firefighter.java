package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.Map;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.extension.util.chat.Chat;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fire;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;

public class Firefighter extends MicroGame implements Listener {

    protected static final Integer SPEC_LOCATION_INDEX = 0;
    protected static final Integer START_LOCATION_INDEX = 1;
    protected static final Integer FIRST_CORNER_INDEX = 2;
    protected static final Integer SECOND_CORNER_INDEX = 3;

    private static final Integer DURATION = 30;
    private static final double FIRE_CHANCE = 0.2;

    private static final List<Tuple<Vector, BlockFace>> NEAR_BLOCKS = List.of(new Tuple<>(new Vector(1, 0, 0), BlockFace.WEST), new Tuple<>(new Vector(-1, 0, 0), BlockFace.EAST), new Tuple<>(new Vector(0, 1, 0), null), new Tuple<>(new Vector(0, -1, 0), BlockFace.UP), new Tuple<>(new Vector(0, 0, 1), BlockFace.NORTH), new Tuple<>(new Vector(0, 0, -1), BlockFace.SOUTH));

    private final HashMap<User, Integer> punchedOutNumberByUser = new HashMap<>();
    private BukkitTask timeTask;

    public Firefighter() {
        super("firefighter", "Firefighter", Material.BLAZE_POWDER, "Punch out the fire", 1);

        Server.registerListener(this, GameMicroGames.getPlugin());
    }

    @Override
    public Integer getLocationAmount() {
        return 2;
    }

    @Override
    public void onMapLoad(Map map) {
        super.onMapLoad(map);

        ExWorld world = map.getWorld();

        world.allowFirePunchOut(true);
        world.allowFlintAndSteel(false);
        world.allowBlockBurnUp(false);
        world.allowPlayerDamage(false);
        world.allowBlockBreak(false);
        world.allowBlockPlace(false);
        world.allowLightUpInteraction(false);
        world.allowFireSpread(false);
    }

    @Override
    public void load() {
        super.load();

        super.sideboard.setScore(4, "§9§lTime");
        super.sideboard.setScore(3, "§f0s");
        super.sideboard.setScore(2, "§f-------------------");
        super.sideboard.setScore(1, "§c§lPunched out fires");
        super.sideboard.setScore(0, "§f0");
    }

    @Override
    protected void loadDelayed() {
        for (User user : Server.getPreGameUsers()) {
            user.teleport(this.getStartLocation());
        }

        this.spreadFire();
    }

    private void spreadFire() {
        ExWorld world = this.currentMap.getWorld();
        ExLocation first = this.getFirstCorner();
        ExLocation second = this.getSecondCorner();

        for (int x = first.getBlockX(); x < second.getBlockX(); x++) {
            for (int y = first.getBlockY(); y < second.getBlockY(); y++) {
                for (int z = first.getBlockZ(); z < second.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isBurnable()) {
                        continue;
                    }

                    for (Tuple<Vector, BlockFace> tuple : NEAR_BLOCKS) {
                        Vector vec = tuple.getA();
                        BlockFace blockFace = tuple.getB();

                        Block nearBlock = block.getLocation().add(vec).getBlock();

                        if (!nearBlock.isEmpty()) {
                            continue;
                        }

                        if (this.random.nextDouble() < FIRE_CHANCE) {
                            nearBlock.setType(Material.FIRE);
                            if (blockFace != null) {
                                BlockData blockData = nearBlock.getBlockData();
                                if (blockData instanceof Fire) {
                                    ((Fire) blockData).setFace(blockFace, true);
                                    nearBlock.setBlockData(blockData);
                                }

                            }

                        }
                    }
                }
            }
        }
    }

    @Override
    public void start() {
        super.start();

        for (User user : Server.getInGameUsers()) {
            user.setGameMode(GameMode.SURVIVAL);
        }

        this.timeTask = Server.runTaskTimerSynchrony((time) -> {
            super.sideboard.setScore(3, Chat.getTimeString(time));

            if (time == 0) {
                this.stop();
            }
        }, DURATION, true, 0, 20, GameMicroGames.getPlugin());
    }

    @Override
    public void stop() {
        if (this.timeTask != null) {
            this.timeTask.cancel();
        }

        super.calcPlaces(this.punchedOutNumberByUser::get, true);
        super.stop();
    }

    @Override
    public void reset() {
        super.reset();

        if (this.previousMap != null) {
            Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
        }

        this.punchedOutNumberByUser.clear();
    }

    @Override
    public boolean hasSideboard() {
        return true;
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
        return this.currentMap.getLocation(SPEC_LOCATION_INDEX);
    }

    @Override
    public ExLocation getStartLocation() {
        return this.currentMap.getLocation(START_LOCATION_INDEX);
    }

    public ExLocation getFirstCorner() {
        return this.currentMap.getLocation(FIRST_CORNER_INDEX);
    }

    public ExLocation getSecondCorner() {
        return this.currentMap.getLocation(SECOND_CORNER_INDEX);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        User user = Server.getUser(e.getPlayer());

        if (user == null || !user.getStatus().equals(Status.User.IN_GAME) || !this.isGameRunning()) {
            e.setCancelled(true);
            return;
        }

        if (e.getClickedBlock() == null || !e.getClickedBlock().getType().equals(Material.FIRE)) {
            return;
        }

        int number = this.punchedOutNumberByUser.compute(user, (u, v) -> v == null ? 1 : v + 1);
        user.setSideboardScore(0, "§f" + number);

    }


}
