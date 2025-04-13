package jonyboylovespie.lockedchestplugin.lockedChestsPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ChestOwnershipListener implements Listener, CommandExecutor
{
    private JavaPlugin plugin;
    private NamespacedKey ownerKey;
    private NamespacedKey trustedKey;

    public ChestOwnershipListener(JavaPlugin plugin)
    {
        this.plugin = plugin;
        ownerKey = new NamespacedKey(plugin, "owner");
        trustedKey = new NamespacedKey(plugin, "trusted");
        this.plugin.getCommand("trust").setExecutor(this);
        this.plugin.getCommand("lockchest").setExecutor(this);
        this.plugin.getCommand("lockinspect").setExecutor(this);
    }

    // Data management methods

    private void removeOwnership(Chest chest)
    {
        chest.getPersistentDataContainer().remove(ownerKey);
        chest.getPersistentDataContainer().remove(trustedKey);
        chest.update();
    }

    private void setOwnership(Player owner, Chest chest)
    {
        setOwnership(owner.getUniqueId().toString(), chest);
    }

    private void setOwnership(String owner, Chest chest)
    {
        chest.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner);
        chest.update();
    }

    private String getOwner(Chest chest)
    {
        return chest.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
    }

    private void addTrustedPlayer(Player trustedPlayer, Chest chest)
    {
        addTrustedPlayer(trustedPlayer.getUniqueId().toString(), chest);
    }

    private void addTrustedPlayer(String trustedPlayer, Chest chest)
    {
        StringListDataType stringListType = new StringListDataType();
        List<String> trusted = getTrustedPlayers(chest);
        if (trusted == null)
        {
            trusted = new ArrayList<>();
            trusted.add(trustedPlayer);
        }
        else
        {
            trusted.add(trustedPlayer);
        }
        chest.getPersistentDataContainer().set(trustedKey, stringListType, trusted);
        chest.update();
    }

    private List<String> getTrustedPlayers(Chest chest)
    {
        StringListDataType stringListType = new StringListDataType();
        return chest.getPersistentDataContainer().get(trustedKey, stringListType);
    }

    // Permission checking methods

    private boolean canPlayerOpen(Player player, Chest chest)
    {
        return isPlayerOwner(player, chest) || isPlayerTrusted(player, chest);
    }

    private boolean isPlayerTrusted(Player player, Chest chest)
    {
        List<String> trustedPlayers = getTrustedPlayers(chest);
        if (trustedPlayers == null) return false;
        return trustedPlayers.contains(player.getUniqueId().toString());
    }

    private boolean isPlayerOwner(Player player, Chest chest)
    {
        String owner = getOwner(chest);
        if (owner == null) return false;
        UUID ownerUUID = UUID.fromString(owner);
        return ownerUUID.equals(player.getUniqueId());
    }

    // Chest Locking/Unlocking

    private void handleLocking(Player player, Chest chest)
    {
        String owner = getOwner(chest);
        if (owner == null)
        {
            handleAddLock(player, chest);
            return;
        }
        handleRemoveLock(player, chest);
    }

    private void handleAddLock(Player player, Chest chest)
    {
        String ownerUUID = getOwner(chest);
        if (ownerUUID == null)
        {
            if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest)
            {
                setOwnership(player, (Chest) doubleChest.getLeftSide());
                setOwnership(player, (Chest) doubleChest.getRightSide());
            }
            else
            {
                setOwnership(player, chest);
            }
            player.sendMessage(ChatColor.GREEN + "Locking chest.");
            return;
        }
        if (!isPlayerOwner(player, chest))
        {
            player.sendMessage(ChatColor.RED + "You cannot lock this chest, it is locked by " + Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName());
            return;
        }
        player.sendMessage(ChatColor.RED + "This chest is already locked by you.");
    }

    private void handleRemoveLock(Player player, Chest chest)
    {
        String ownerUUID = getOwner(chest);
        if (ownerUUID == null)
        {
            player.sendMessage(ChatColor.RED + "This chest is not locked.");
            return;
        }
        if (!isPlayerOwner(player, chest) && !player.isOp())
        {
            player.sendMessage(ChatColor.RED + "You cannot unlock this chest, it is locked by " + Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName());
            return;
        }
        if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest)
        {
            removeOwnership((Chest) doubleChest.getLeftSide());
            removeOwnership((Chest) doubleChest.getRightSide());
        }
        else
        {
            removeOwnership(chest);
        }
        player.sendMessage(ChatColor.GREEN + "Unlocking chest.");
    }

    // Event handling methods

    @EventHandler
    public void onBlockPhysicsEvent(BlockPhysicsEvent event)
    {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;
        Chest chest = (Chest) block.getState();
        if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest)
        {
            String owner = getOwner(chest);
            Chest newChest = (Chest) (chest.equals(doubleChest.getRightSide()) ? doubleChest.getLeftSide() : doubleChest.getRightSide());
            if (owner == null) return;
            setOwnership(owner, newChest);
            List<String> trustedPlayers = getTrustedPlayers(chest);
            if (trustedPlayers == null) return;
            trustedPlayers.forEach(trustedPlayer -> addTrustedPlayer(trustedPlayer, newChest));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;
        Chest chest = (Chest) block.getState();
        String owner = getOwner(chest);
        if (owner == null) return;
        Player player = event.getPlayer();
        UUID ownerUUID = UUID.fromString(owner);
        if (!ownerUUID.equals(player.getUniqueId()))
        {
            player.sendMessage(ChatColor.RED + "You cannot break this chest, it is locked by " + Bukkit.getOfflinePlayer(ownerUUID).getName());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event)
    {
        Location location = event.getSource().getLocation();
        if (location == null) return;
        Block sourceBlock = location.getBlock();
        if (sourceBlock.getType() != Material.CHEST) return;
        Chest chest = (Chest) sourceBlock.getState();
        if (getOwner(chest) != null)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.CHEST) return;
        Player player = event.getPlayer();
        Chest chest = (Chest) block.getState();
        handleChestInteraction(player, chest, event);
    }

    private void handleChestInteraction(Player player, Chest chest, PlayerInteractEvent event)
    {
        String owner = getOwner(chest);
        if (isUsingKey(player))
        {
            handleLocking(player, chest);
            return;
        }
        if (owner == null) return;
        if (canPlayerOpen(player, chest)) return;
        UUID ownerUUID = UUID.fromString(owner);
        player.sendMessage(ChatColor.RED + "You cannot open this chest, it is locked by " + Bukkit.getOfflinePlayer(ownerUUID).getName());
        event.setCancelled(true);
    }

    // Command handling methods

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player)) return false;
        if (command.getName().equalsIgnoreCase("trust")) return handleTrustCommand(player, args);
        if (command.getName().equalsIgnoreCase("lockchest")) return handleLockChestCommand(player, args);
        if (command.getName().equalsIgnoreCase("lockinspect")) return handleLockInspectCommand(player, args);
        return false;
    }

    private boolean handleTrustCommand(Player player, String[] args)
    {
        if (args.length != 1) return false;
        String playerName = args[0];
        Player trustedPlayer = Bukkit.getPlayer(playerName);
        if (trustedPlayer == null)
        {
            player.sendMessage(ChatColor.RED + playerName + " is not online.");
            return false;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null || block.getType() != Material.CHEST)
        {
            player.sendMessage(ChatColor.RED + "You must be looking at a chest.");
            return true;
        }
        Chest chest = (Chest) block.getState();
        if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest)
        {
            addTrustedPlayer(trustedPlayer, (Chest) doubleChest.getLeftSide());
            addTrustedPlayer(trustedPlayer, (Chest) doubleChest.getRightSide());
        }
        else
        {
            addTrustedPlayer(trustedPlayer, chest);
        }
        player.sendMessage(ChatColor.GREEN + "Trusted " + playerName + " to this chest.");
        return true;
    }

    private boolean handleLockChestCommand(Player player, String[] args)
    {
        if (args.length != 1) return false;
        String action = args[0].toLowerCase();
        if (!action.equals("add") && !action.equals("remove"))
        {
            player.sendMessage(ChatColor.RED + "Must proceed /lockchest with add or remove.");
            return false;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null || block.getType() != Material.CHEST)
        {
            player.sendMessage(ChatColor.RED + "You must be looking at a chest.");
            return true;
        }
        Chest chest = (Chest) block.getState();
        if (action.equals("add"))
        {
            handleAddLock(player, chest);
            return true;
        }
        handleRemoveLock(player, chest);
        return true;
    }

    private boolean handleLockInspectCommand(Player player, String[] args)
    {
        if (args.length != 0) return false;
        Block block = player.getTargetBlockExact(5);
        if (block == null || block.getType() != Material.CHEST)
        {
            player.sendMessage(ChatColor.RED + "You must be looking at a chest.");
            return true;
        }
        Chest chest = (Chest) block.getState();
        String owner = getOwner(chest);
        if (owner == null)
        {
            player.sendMessage(ChatColor.RED + "This chest is not locked.");
            return true;
        }
        Location chestLocation = chest.getLocation();
        player.sendMessage(ChatColor.BLUE + "Chest info of chest at " + chestLocation.getX() + ", " + chestLocation.getY() + ", " + chestLocation.getZ() + " " + chestLocation.getWorld().getName() + ":");
        player.sendMessage(ChatColor.YELLOW + "Owner:");
        player.sendMessage(Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName());
        player.sendMessage(ChatColor.YELLOW + "Trusted players:");
        List<String> trustedPlayers = getTrustedPlayers(chest);
        if (trustedPlayers == null)
        {
            player.sendMessage("No trusted players.");
            return true;
        }
        for (String trustedPlayer : trustedPlayers)
        {
            player.sendMessage(Bukkit.getOfflinePlayer(UUID.fromString(trustedPlayer)).getName());
        }
        return true;
    }

    // Key methods

    private boolean isUsingKey(Player player)
    {
        return player.isSneaking() && playerHasKey(player);
    }

    private boolean playerHasKey(Player player)
    {
        return isKeyItem(player.getInventory().getItemInMainHand()) || isKeyItem(player.getInventory().getItemInOffHand());
    }

    private boolean isKeyItem(ItemStack item)
    {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equalsIgnoreCase("Key") && (item.getType() == Material.IRON_NUGGET || item.getType() == Material.GOLD_NUGGET);
    }
}