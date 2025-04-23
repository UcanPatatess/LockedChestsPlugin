package jonyboylovespie.lockedchestplugin.lockedChestsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

public class ExplosionListener implements Listener
{
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event)
    {
        event.blockList().removeIf(BlockSecurityListener::isChest);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event)
    {
        event.blockList().removeIf(BlockSecurityListener::isChest);
    }
}