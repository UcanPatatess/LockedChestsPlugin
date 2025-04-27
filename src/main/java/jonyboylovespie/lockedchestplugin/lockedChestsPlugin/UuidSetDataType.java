package jonyboylovespie.lockedchestplugin.lockedChestsPlugin;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UuidSetDataType implements PersistentDataType<byte[], Set<UUID>>
{

    @Override
    public Class<byte[]> getPrimitiveType()
    {
        return byte[].class;
    }

    @Override
    public Class<Set<UUID>> getComplexType()
    {
        return (Class<Set<UUID>>) (Class<?>) Set.class;
    }

    @Override
    public byte[] toPrimitive(Set<UUID> uuids, PersistentDataAdapterContext context)
    {
        int count = uuids == null ? 0 : uuids.size();
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES + count * Long.BYTES * 2);
        buf.putInt(count);
        if (uuids != null)
        {
            for (UUID u : uuids)
            {
                buf.putLong(u.getMostSignificantBits());
                buf.putLong(u.getLeastSignificantBits());
            }
        }
        return buf.array();
    }

    @Override
    public Set<UUID> fromPrimitive(byte[] raw, PersistentDataAdapterContext context)
    {
        if (raw == null || raw.length < Integer.BYTES)
        {
            return new HashSet<>();
        }
        ByteBuffer buf = ByteBuffer.wrap(raw);
        int count = buf.getInt();
        if (count < 0 || raw.length != Integer.BYTES + count * Long.BYTES * 2)
        {
            return new HashSet<>();
        }
        Set<UUID> result = new HashSet<>(count);
        for (int i = 0; i < count; i++)
        {
            result.add(new UUID(buf.getLong(), buf.getLong()));
        }
        return result;
    }
}