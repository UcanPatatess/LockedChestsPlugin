package jonyboylovespie.lockedchestplugin.lockedChestsPlugin;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StringListDataType implements PersistentDataType<byte[], List<String>> {
    @Override
    public Class<byte[]> getPrimitiveType()
    {
        return byte[].class;
    }

    @Override
    public Class<List<String>> getComplexType()
    {
        return (Class<List<String>>) (Object) List.class;
    }

    @Override
    public byte[] toPrimitive(List<String> strings, PersistentDataAdapterContext context)
    {
        try
        {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(strings);
            return byteOut.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return new byte[0];
        }
    }

    @Override
    public List<String> fromPrimitive(byte[] bytes, PersistentDataAdapterContext context)
    {
        try
        {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(byteIn);
            return (List<String>) in.readObject();
        }
        catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}