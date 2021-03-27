package me.bigratenthusiast.crypt

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object Utils {
    fun playerClear(player: Player){
        player.inventory.clear()
        player.gameMode = GameMode.SURVIVAL
        player.activePotionEffects.clear()
        player.teleport(Location(player.world, "-127".toDouble(), "143".toDouble(), "-369".toDouble()), PlayerTeleportEvent.TeleportCause.PLUGIN)
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 100, 255))
        player.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, 100, 255))
    }
}