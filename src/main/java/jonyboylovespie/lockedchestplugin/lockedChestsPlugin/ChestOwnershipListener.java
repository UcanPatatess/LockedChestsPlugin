package jonyboylovespie.lockedchestplugin.lockedChestsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
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

    private void removeOwnership(Chest chest)
    {
        chest.getPersistentDataContainer().remove(ownerKey);
        chest.getPersistentDataContainer().remove(trustedKey);
        if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest)
        {
            Chest otherPart = (Chest) (chest.equals(doubleChest.getRightSide()) ? doubleChest.getLeftSide() : doubleChest.getRightSide());
            otherPart.getPersistentDataContainer().remove(ownerKey);
            otherPart.getPersistentDataContainer().remove(trustedKey);
            otherPart.update();
        }
        chest.update();
    }

    private void setOwnership(Player owner, Chest chest)
    {
        chest.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        chest.update();
    }

    private void addTrustedPlayer(Player player, Chest chest)
    {
        String trustedPlayers = chest.getPersistentDataContainer().get(trustedKey, PersistentDataType.STRING);
        if (trustedPlayers == null)
        {
            trustedPlayers = player.getUniqueId().toString();
        }
        else
        {
            trustedPlayers += "," + player.getUniqueId();
        }
        chest.getPersistentDataContainer().set(trustedKey, PersistentDataType.STRING, trustedPlayers);
        chest.update();
    }

    private String getOwner(Chest chest)
    {
        String owner = chest.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest)
        {
            Chest otherPart = (Chest) (chest.equals(doubleChest.getRightSide()) ? doubleChest.getLeftSide() : doubleChest.getRightSide());
            if (owner == null) owner = otherPart.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        }
        return owner;
    }

    private String[] getTrustedPlayers(Chest chest)
    {
        if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest)
        {
            String trusted = chest.getPersistentDataContainer().get(trustedKey, PersistentDataType.STRING);
            Chest otherPart = (Chest) (chest.equals(doubleChest.getRightSide()) ? doubleChest.getLeftSide() : doubleChest.getRightSide());
            String[] trustedPlayers = new String[0];
            if (trusted != null)
            {
                trustedPlayers = trusted.split(",");
            }
            String otherTrusted = otherPart.getPersistentDataContainer().get(trustedKey, PersistentDataType.STRING);
            if (otherTrusted != null)
            {
                String[] otherTrustedPlayers = otherTrusted.split(",");
                String[] combined = new String[trustedPlayers.length + otherTrustedPlayers.length];
                System.arraycopy(trustedPlayers, 0, combined, 0, trustedPlayers.length);
                System.arraycopy(otherTrustedPlayers, 0, combined, trustedPlayers.length, otherTrustedPlayers.length);
                trustedPlayers = combined;
            }
            String trustedPlayersString = String.join(",", trustedPlayers);
            chest.getPersistentDataContainer().set(trustedKey, PersistentDataType.STRING, trustedPlayersString);
            chest.update();
            otherPart.getPersistentDataContainer().set(trustedKey, PersistentDataType.STRING, trustedPlayersString);
            otherPart.update();
        }
        String finalTrusted = chest.getPersistentDataContainer().get(trustedKey, PersistentDataType.STRING);
        return finalTrusted != null ? finalTrusted.split(",") : new String[0];
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;
        Player player = event.getPlayer();
        Chest chest = (Chest) block.getState();
        String owner = getOwner(chest);
        if (owner == null) return;
        UUID ownerUUID = UUID.fromString(owner);
        if (!ownerUUID.equals(player.getUniqueId()))
        {
            player.sendMessage("You cannot break this chest, it is locked by " + Bukkit.getOfflinePlayer(ownerUUID).getName());
            event.setCancelled(true);
        }
        if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest)
        {
            Chest otherPart = (Chest) (chest.equals(doubleChest.getRightSide()) ? doubleChest.getLeftSide() : doubleChest.getRightSide());
            setOwnership(player, otherPart);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event)
    {
        Block sourceBlock = event.getSource().getLocation().getBlock();
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
        UUID playerUUID = player.getUniqueId();
        String owner = getOwner(chest);
        String[] trustedPlayers = getTrustedPlayers(chest);
        if (player.isSneaking() && isKeyItem(player.getInventory().getItemInMainHand()) || isKeyItem(player.getInventory().getItemInOffHand()))
        {
            handleLocking(player, chest);
            return;
        }
        if (owner == null) return;
        UUID ownerUUID = UUID.fromString(owner);
        if (ownerUUID.equals(playerUUID) || isPlayerTrusted(playerUUID.toString(), trustedPlayers)) return;
        player.sendMessage("You cannot open this chest, it is locked by " + Bukkit.getOfflinePlayer(ownerUUID).getName());
        event.setCancelled(true);
    }

    private boolean isPlayerTrusted(String playerUUID, String[] trustedPlayers)
    {
        for (String trustedPlayer : trustedPlayers)
        {
            if (trustedPlayer.equals(playerUUID)) return true;
        }
        return false;
    }

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

    private boolean isKeyItem(ItemStack item)
    {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equalsIgnoreCase("Key") && (item.getType() == Material.IRON_NUGGET || item.getType() == Material.GOLD_NUGGET);
    }

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
        Block block = player.getTargetBlockExact(5);
        if (block == null || block.getType() != Material.CHEST)
        {
            player.sendMessage("You must be looking at a chest.");
            return true;
        }
        Chest chest = (Chest) block.getState();
        if (!isPlayerOwner(player, chest))
        {
            player.sendMessage("You do not own this chest.");
            return true;
        }
        Player trustedPlayer = Bukkit.getPlayer(args[0]);
        if (trustedPlayer == null)
        {
            player.sendMessage("Player " + args[0] + " is not online.");
            return true;
        }
        for (String playerUUID : getTrustedPlayers(chest))
        {
            if (trustedPlayer.getUniqueId().toString().equals(playerUUID))
            {
                player.sendMessage("Player " + trustedPlayer.getName() + " is already trusted.");
                return true;
            }
        }
        addTrustedPlayer(trustedPlayer, chest);
        player.sendMessage("Player " + trustedPlayer.getName() + " has been trusted.");
        return true;
    }

    private boolean handleLockChestCommand(Player player, String[] args)
    {
        if (args.length != 1) return false;
        String action = args[0].toLowerCase();
        if (!action.equals("add") && !action.equals("remove"))
        {
            player.sendMessage("Must follow /lockchest with add or remove.");
            return false;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null || block.getType() != Material.CHEST)
        {
            player.sendMessage("You must be looking at a chest.");
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
            player.sendMessage("You must be looking at a chest.");
            return true;
        }
        Chest chest = (Chest) block.getState();
        String owner = getOwner(chest);
        if (owner == null)
        {
            player.sendMessage("This chest is not locked.");
            return true;
        }
        player.sendMessage(owner);
        return true;
    }

    private void handleAddLock(Player player, Chest chest)
    {
        String ownerUUID = getOwner(chest);
        if (ownerUUID == null)
        {
            setOwnership(player, chest);
            player.sendMessage("Locking chest.");
            return;
        }
        if (!isPlayerOwner(player, chest))
        {
            player.sendMessage("You cannot lock this chest, it is locked by " + Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName());
            return;
        }
        player.sendMessage("This chest is already locked by you.");
    }

    private void handleRemoveLock(Player player, Chest chest)
    {
        String ownerUUID = getOwner(chest);
        if (ownerUUID == null)
        {
            setOwnership(player, chest);
            player.sendMessage("This chest is not locked.");
            return;
        }
        if (!isPlayerOwner(player, chest) && !player.isOp())
        {
            player.sendMessage("You cannot unlock this chest, it is locked by " + Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName());
            return;
        }
        removeOwnership(chest);
        player.sendMessage("Unlocking chest.");
    }

    private boolean isPlayerOwner(Player player, Chest chest)
    {
        String owner = getOwner(chest);
        if (owner == null) return false;
        UUID ownerUUID = UUID.fromString(owner);
        return ownerUUID.equals(player.getUniqueId());
    }
}