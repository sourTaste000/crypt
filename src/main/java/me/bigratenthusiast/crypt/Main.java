package me.bigratenthusiast.crypt;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Main extends JavaPlugin implements Listener {
    // All cooldowns are in ticks (originally i thought it was ms and got trolled)
    public static long totemCooldown = 1200L;

    public static long dashCooldown = 620L;
    public static long untilSlow = 200L;

    public static CopyOnWriteArrayList<UUID> dashCooldowns = new CopyOnWriteArrayList<>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("quickheal")) {
            if (args[0].isEmpty()) {
                sender.sendMessage("You must specify a player.");
                return false;
            }
            if (!sender.isOp()) {
                sender.sendMessage("You do not have permission to use this command.");
                return false;
            }
            Player player = getServer().getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage("There is no player by that name.");
                return false;
            } else {
                player.setHealth(player.getMaxHealth()); // TODO: deprecated replace with .getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                player.setFoodLevel(20);
                player.setSaturation(20);
                Bukkit.broadcastMessage(sender.getName() + " used quickheal on " + args[0]);
            }

        } else if (label.equalsIgnoreCase("deathmatch")) {
            if (!sender.isOp()) {
                sender.sendMessage("You do not have permission to use this command.");
                return false;
            }
            Bukkit.broadcastMessage("Deathmatch initiated by " + sender.getName());
            getServer().getOnlinePlayers().forEach(player -> {
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    Random random = new Random();
                    player.teleport(new Location(player.getWorld(), random.nextInt(10) - 65, 83, random.nextInt(16) - 346), PlayerTeleportEvent.TeleportCause.END_PORTAL);
                }
            });
        } else if (label.equalsIgnoreCase("rematch")) {
            if (!sender.isOp()) {
                sender.sendMessage("You do not have permission to use this command.");
                return false;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=!minecraft:player,type=!minecraft:villager,type=!minecraft:painting,type=!minecraft:item_frame]");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @e");
            getServer().getOnlinePlayers().forEach(Utils::playerClear);
        } else if (label.equalsIgnoreCase("killall")) {
            if (!sender.isOp()) {
                sender.sendMessage("You do not have permission to use this command.");
                return false;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=!minecraft:player,type=!minecraft:villager,type=!minecraft:painting,type=!minecraft:item_frame]");
        }

        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getEntity().setGameMode(GameMode.SPECTATOR);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            if (event.getPlayer().getLocation().getBlockY() > 240) {
                event.getPlayer().teleport(event.getPlayer().getLocation().subtract(0, 2, 0));
                event.getPlayer().sendMessage("You cannot go any higher than this.");
            } else if (event.getPlayer().getLocation().getBlockY() < 60) {
                Player player = event.getPlayer();
                player.setFallDistance(0);
                Utils.playerClear(player);
                player.sendMessage("Whoops!");
            }
        }
    }

    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && !event.isCancelled()) {
            Player player = (Player) event.getEntity();
            ItemStack hollowTotem = new ItemStack(Material.FIREWORK_STAR);
            ItemMeta im = hollowTotem.getItemMeta();
            im.setDisplayName("§rHollow Totem");
            hollowTotem.setItemMeta(im);

            if (player.getInventory().getItemInOffHand().getType() == Material.AIR) {
                player.getInventory().setItemInOffHand(hollowTotem);
            } else {
                player.getInventory().addItem(hollowTotem);
            }
            player.sendMessage("Your totem will recharge in one minute.");
            this.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    player.sendMessage("Your totem is now recharged.");
                    player.getInventory().forEach(possibleHollow -> {
                        if (possibleHollow != null && possibleHollow.getType() == Material.FIREWORK_STAR) {
                            ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
                            possibleHollow.setType(Material.TOTEM_OF_UNDYING);
                            possibleHollow.setItemMeta(totem.getItemMeta());
                        }
                    });
                }
            }, totemCooldown);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR && event.getPlayer().isSneaking()) {
            Player player = event.getPlayer();
            if (player.getInventory().getBoots().getItemMeta().getDisplayName().contains("Rush Boots") && player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_AXE) {
                if (dashCooldowns.contains(player.getUniqueId())) {
                    player.sendMessage("You are on cooldown to use that ability.");
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 4, true, false));
                    player.playEffect(EntityEffect.BREAK_EQUIPMENT_CHESTPLATE);
                    this.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 120, 2, true, false));
                    }, untilSlow);
                    dashCooldowns.add(player.getUniqueId());
                    this.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                        dashCooldowns.remove(player.getUniqueId());
                        player.sendMessage("You can now use that ability.");
                    }, dashCooldown);
                }

            }
        }
    }

}
