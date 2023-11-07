package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.CancelPriority;
import de.timesnake.basic.bukkit.util.user.event.UserBlockBreakEvent;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RiseUp extends ScoreGame<Integer> implements Listener {

    private BukkitTask scoreTask;

    public RiseUp() {
        super("rise_up",
                "Rise Up",
                Material.ELYTRA,
                "Build up and stay there",
                List.of("§hGoal: §pbe the highest player when the timer is up",
                        "Build up using the provided blocks.",
                        "You can use snowballs to knock other players down.",
                        "When the time is up the order of the players is determined by the height they are standing at."
                        ),
                1,
                Duration.ofSeconds(120));
        Server.registerListener(this, GameMicroGames.getPlugin());
    }

    @Override
    public void onMapLoad(Map map) {
        super.onMapLoad(map);

        ExWorld world = map.getWorld();

        world.restrict(ExWorld.Restriction.NO_PLAYER_DAMAGE, false);
        world.restrict(ExWorld.Restriction.BLOCK_BREAK, true);
        world.restrict(ExWorld.Restriction.BLOCK_PLACE, false);
        world.restrict(ExWorld.Restriction.DROP_PICK_ITEM, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setPVP(true);
    }

    @Override
    public void start() {
        super.start();

        for (User user : Server.getInGameUsers()) {
            user.setGameMode(GameMode.SURVIVAL);
            user.unlockInventory();
        }

        this.scoreTask = Server.runTaskTimerAsynchrony(this::updateScores, 0, 10, GameMicroGames.getPlugin());
    }

    private void updateScores() {
        for (User user : Server.getInGameUsers()) {
            this.updateUserScore(user, (u, score) -> u.getLocation().getBlockY() - this.getStartLocation().getBlockY());
        }
    }

    @Override
    protected void loadDelayed() {
        super.loadDelayed();

        List<Material> materials = new ArrayList<>(Tag.WOOL.getValues());

        for (User user: Server.getPreGameUsers()) {
            ExItemStack blocks = new ExItemStack(materials.get(this.random.nextInt(materials.size()))).asQuantity(64);
            user.setItem(0, blocks.cloneWithoutId());
            user.setItem(1, blocks.cloneWithoutId());
            user.setItem(2, new ExItemStack(Material.SNOWBALL).asQuantity(16).cloneWithoutId());
            user.setItem(3, new ExItemStack(Material.SHEARS).unbreakable().cloneWithoutId());
            user.lockInventory();
        }

    }

    @Override
    public void stop() {
        if (this.scoreTask != null) {
            this.scoreTask.cancel();
        }

        this.updateScores();
        this.calcPlaces(true);
        super.stop();
    }

    @Override
    public void reset() {
        super.reset();

        if (this.previousMap != null) {
            Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
        }
    }

    @Override
    public boolean hasSideboard() {
        return true;
    }

    @Override
    public Integer getDefaultScore() {
        return 0;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        if (!e.getEntity().getWorld().getPVP()) {
            return;
        }

        if (e.getHitEntity() != null && e.getHitEntity() instanceof Player) {
            ((Player) e.getHitEntity()).damage(0.01, e.getEntity());
            e.getHitEntity().setVelocity(e.getEntity().getVelocity().normalize().multiply(1));
        }
    }

    @EventHandler
    public void onBlockBreak(UserBlockBreakEvent e) {
        if (!this.isGameRunning()) {
            return;
        }

        User user = e.getUser();

        if (user.isService()) {
            return;
        }

        if (Tag.WOOL.isTagged(e.getBlock().getType())) {
            e.setCancelled(CancelPriority.HIGH, false);
        }
    }
}
