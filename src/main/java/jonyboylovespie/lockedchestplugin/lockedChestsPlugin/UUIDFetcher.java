package jonyboylovespie.lockedchestplugin.lockedChestsPlugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class UUIDFetcher
{
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    public static UUID getUUID(String username) throws Exception
    {
        URL url = new URL(MOJANG_API_URL + username);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setReadTimeout(5000);
        con.connect();

        if (con.getResponseCode() != 200)
        {
            return null;
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
        {
            JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
            String rawId = json.get("id").getAsString();
            String dashed = rawId.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
            return UUID.fromString(dashed);
        }
    }
}