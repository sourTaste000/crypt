package me.bigratenthusiast.crypt

import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

const val totemCoolDown: Long = 900
const val dashCoolDown: Long = 620
const val untilSlow: Long = 200
val dashCoolDowns: CopyOnWriteArrayList<UUID> = CopyOnWriteArrayList()


class Main : JavaPlugin(), Listener, CommandExecutor {
    override fun onEnable() {
        this.server.pluginManager.registerEvents(this, this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.name.toLowerCase()){
            "quickheal" -> {
                if (args[0].isEmpty()) {
                    sender.sendMessage("${ChatColor.RED}ERROR: You must specify a player!")
                    return false
                }

                val player: Player = server.getPlayer(args[0]) ?: kotlin.run {
                    sender.sendMessage("There is no player by that name.")
                    return false
                }

                player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 100, 255))
                player.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, 100, 255))
                Bukkit.broadcastMessage("${ChatColor.LIGHT_PURPLE}${sender.name} used quick heal on ${player.name}")
                return true
            }

            "deathmatch" -> {
                Bukkit.broadcastMessage("${ChatColor.RED}Death match initiated by ${sender.name}")
                server.onlinePlayers.forEach{player ->
                    if (player.gameMode == GameMode.SURVIVAL){
                        server.dispatchCommand(server.consoleSender, "spreadplayers -65 -346 10 5 under 85 true @a[gamemode=survival]")
                    }
                }
                return true
            }

            "rematch" -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=!minecraft:player,type=!minecraft:villager,type=!minecraft:painting,type=!minecraft:item_frame]")
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @e")
                server.onlinePlayers.forEach{Utils.playerClear(it)}
                return true
            }

            "killall" -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=!minecraft:player,type=!minecraft:villager,type=!minecraft:painting,type=!minecraft:item_frame]")
            }

            else -> {
                return false
            }
        }
        return true
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {event.entity.gameMode = GameMode.SPECTATOR}

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (player.gameMode == GameMode.SURVIVAL) {
            if (player.location.blockY > 240) {
                player.teleport(player.location.subtract(0.toDouble(), 2.toDouble(), 0.toDouble()))
                player.sendMessage("${ChatColor.RED}You cannot go any higher than this!")
            }
            if (player.location.blockY < 60) {
                player.fallDistance = 0.toFloat()
                Utils.playerClear(player)
                player.sendMessage("${ChatColor.YELLOW}Whoops!")
            }
        }
    }

    @EventHandler
    fun onTotemPop(event: EntityResurrectEvent) {
        if (event.entityType == EntityType.PLAYER && !event.isCancelled) {
            val player = event.entity as Player
            val hollowTotem = ItemStack(Material.FIREWORK_STAR)
            val im = hollowTotem.itemMeta
            im?.setDisplayName("§rHollow Totem")  ?: return
            hollowTotem.itemMeta = im
            if (player.inventory.itemInOffHand.type == Material.AIR) {
                player.inventory.setItemInOffHand(hollowTotem)
            } else {
                player.inventory.addItem(hollowTotem)
            }
            player.sendMessage("Your totem will recharge in forty-five seconds.")
            server.scheduler.scheduleSyncDelayedTask(this, {
                if (player.gameMode == GameMode.SURVIVAL) {
                    player.sendMessage("Your totem is now recharged.")
                    player.inventory.forEach(Consumer { possibleHollow: ItemStack? ->
                        if (possibleHollow != null && possibleHollow.type == Material.FIREWORK_STAR) {
                            val totem = ItemStack(Material.TOTEM_OF_UNDYING)
                            possibleHollow.type = Material.TOTEM_OF_UNDYING
                            possibleHollow.itemMeta = totem.itemMeta
                        }
                    })
                }
            }, totemCoolDown)
        }
    }

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_AIR && event.player.isSneaking) {
            val player = event.player
            if (player.inventory.boots?.itemMeta?.displayName?.contains("Rush Boots") ?: kotlin.run { return } && player.inventory.itemInMainHand.type == Material.GOLDEN_AXE) {
                if (dashCoolDowns.contains(player.uniqueId)) {
                    player.sendMessage("You are on cooldown to use that ability.")
                } else {
                    player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 200, 4, true, false))
                    player.playEffect(EntityEffect.BREAK_EQUIPMENT_CHESTPLATE)
                    server.scheduler.scheduleSyncDelayedTask(this,
                        { player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 120, 2, true, false)) },
                        untilSlow
                    )
                    dashCoolDowns.add(player.uniqueId)
                    server.scheduler.scheduleSyncDelayedTask(this, {
                        dashCoolDowns.remove(player.uniqueId)
                        player.sendMessage("You can now use that ability.")
                    }, dashCoolDown)
                }
            }
        }
    }
}
