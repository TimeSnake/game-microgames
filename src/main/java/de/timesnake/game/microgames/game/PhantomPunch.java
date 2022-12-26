/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.ExItemStack;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.entities.EntityManager;
import de.timesnake.library.entities.entity.bukkit.ExPhantom;
import de.timesnake.library.entities.pathfinder.custom.ExCustomPathfinderGoalPhantomTarget;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PhantomPunch extends ShrinkingPlatformGame implements Listener {

    public static final Integer SPAWN_LOCATION_INDEX = 2;
    public static final Integer START_RADIUS = 3;
    public static final Integer MIN_RADIUS = 1;
    public static final Integer DECREASE_DELAY = 40;
    public static final Integer SPAWN_DELAY = 11;
    public static final Integer START_PHANTOMS = 6;
    public static final ExItemStack BOW =
            new ExItemStack(Material.BOW).unbreakable().addEnchantments(new Tuple<>(Enchantment.ARROW_INFINITE, 1));
    protected static final Integer SPEC_LOCATION_INDEX = 0;
    protected static final Integer START_LOCATION_INDEX = 1;
    private final List<ExPhantom> phantoms = new ArrayList<>();
    private BukkitTask spawnTask;

    public PhantomPunch() {
        super("phantompunch", "PhantomPunch", Material.PHANTOM_MEMBRANE, "Try to defend the phantoms, as a team", 1);

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
        super.currentMap.getWorld().setGameRule(GameRule.UNIVERSAL_ANGER, true);

        for (Entity entity : this.currentMap.getWorld().getEntities()) {
            if (entity.getType().equals(EntityType.PHANTOM)) {
                entity.remove();
            }
        }
    }

    @Override
    public void load() {
        super.load();
        super.currentMap.getWorld().setTime(20000);
        this.phantoms.clear();

        super.sideboard.setScore(4, "§c§lPhantoms");
        super.sideboard.setScore(3, "§f" + this.phantoms.size());
        super.sideboard.setScore(2, "§f-------------");
        super.sideboard.setScore(1, "§9§lPlayers");
        super.sideboard.setScore(0, Server.getPreGameUsers().size() + "");
    }

    @Override
    protected void loadDelayed() {
        for (User user : Server.getPreGameUsers()) {
            user.teleport(this.getSpawnLocation());
            user.lockLocation(true);
            user.lockInventoryItemMove();
            user.setItem(0, BOW);
            user.setItem(8, new ItemStack(Material.ARROW));
        }
    }

    @Override
    public void start() {
        super.start();

        for (User user : Server.getInGameUsers()) {
            user.lockLocation(false);
        }

        for (int i = 0; i < START_PHANTOMS; i++) {
            this.spawnPhantom();
        }

        this.spawnTask = Server.runTaskTimerSynchrony(() -> {

                    for (ExPhantom phantom : this.phantoms) {
                        phantom.setTarget(this.getRandomUser().getPlayer());
                    }
                    this.spawnPhantom();

                }, (int) (20 / SPAWN_DELAY * Math.sqrt(Server.getInGameUsers().size())), 20 * SPAWN_DELAY,
                GameMicroGames.getPlugin());

    }

    private void spawnPhantom() {
        Location loc = this.getSpawnLocation();
        ExPhantom phantom = new ExPhantom(this.getSpawnLocation().getWorld(), true, true);
        phantom.setPosition(loc.getX(), loc.getY() + 30, loc.getZ());
        phantom.clearGoalTargets();
        phantom.addPathfinderGoal(1, new ExCustomPathfinderGoalPhantomTarget());
        phantom.setTarget(this.getRandomUser().getPlayer());

        EntityManager.spawnEntity(this.getSpawnLocation().getWorld(), phantom);
        this.phantoms.add(phantom);
        super.sideboard.setScore(3, "§f" + this.phantoms.size());
    }

    private User getRandomUser() {
        List<User> users = new ArrayList<>(Server.getInGameUsers());
        return users.get(this.random.nextInt(users.size()));
    }

    @Override
    public void stop() {
        Server.broadcastTitle(Component.text("Game over", ExTextColor.WARNING), Component.empty(), Duration.ofSeconds(3));

        if (this.spawnTask != null) {
            this.spawnTask.cancel();
        }

        for (Entity entity : this.currentMap.getWorld().getEntities()) {
            if (entity instanceof Phantom) {
                entity.remove();
            }
        }

        super.stop();
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
        for (Entity entity : this.currentMap.getWorld().getEntities()) {
            if (entity.getType().equals(EntityType.PHANTOM)) {
                entity.remove();
            }
        }
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
        if (Server.getInGameUsers().size() == 0) {
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
        return super.currentMap.getLocation(SPAWN_LOCATION_INDEX);
    }

    @EventHandler
    public void onEntityDamage(UserDamageEvent e) {
        if (!this.isGameRunning()) {
            return;
        }
        e.setCancelDamage(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        e.setDamage(0);
    }

    @Override
    public Integer getDeathHeight() {
        return this.getSpawnLocation().getBlockY();
    }

    @EventHandler
    @Override
    public void onUserMove(UserMoveEvent e) {
        User user = e.getUser();

        if (!this.isGameRunning()) {
            return;
        }

        if (user.getStatus().equals(Status.User.IN_GAME)) {
            if (e.getTo().getBlockY() < this.getCenterLocation().getBlockY()) {
                super.onUserMove(e);

                super.sideboard.setScore(0, Server.getInGameUsers().size() + "");

                if (Server.getInGameUsers().size() == 0) {
                    super.sideboard.setScore(0, Server.getInGameUsers().size() + "");
                }
            }
        }
    }

    @Override
    public String getDeathMessage() {
        return " was punched out!";
    }
}
