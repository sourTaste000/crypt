package me.bigratenthusiast.crypt;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Utils {
    public static void playerClear(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.getActivePotionEffects().clear();
        player.teleport(new Location(player.getWorld(), -127, 143, -369), PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setHealth(20);
    }
}
