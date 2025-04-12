package jonyboylovespie.lockedchestplugin.lockedChestsPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import java.util.List;

public class AnvilRenameListener implements Listener
{
    @EventHandler
    public void onAnvilRename(PrepareAnvilEvent event)
    {
        AnvilInventory inventory = event.getInventory();
        ItemStack result = event.getResult();
        if (result == null) return;
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;
        if (result.getType() == Material.IRON_NUGGET || result.getType() == Material.GOLD_NUGGET)
        {
            if (meta.getDisplayName().equalsIgnoreCase("key"))
            {
                CustomModelDataComponent component = meta.getCustomModelDataComponent();
                List<String> strings = List.of("key");
                component.setStrings(strings);
                meta.setCustomModelDataComponent(component);
            }
            result.setItemMeta(meta);
            inventory.setItem(2, result);
        }
    }
}
