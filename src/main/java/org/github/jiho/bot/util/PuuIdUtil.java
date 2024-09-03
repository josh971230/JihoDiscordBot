package org.github.jiho.bot.util;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.github.jiho.bot.Constants;

public class PuuIdUtil {
    private static final String RIOT_API_KEY = Constants.API_KEY;
    private static final String TFT_API_KEY = Constants.TFT_KEY;

    public static String getPuuid(String username, String tag) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String encodedGameName = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String encodedTagLine = URLEncoder.encode(tag, StandardCharsets.UTF_8);

        String puuidUrl = String.format("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s?api_key=%s",
                encodedGameName, encodedTagLine, RIOT_API_KEY);

        HttpRequest puuidRequest = HttpRequest.newBuilder()
                .uri(URI.create(puuidUrl))
                .build();
        HttpResponse<String> puuidResponse = client.send(puuidRequest, HttpResponse.BodyHandlers.ofString());

        if (puuidResponse.statusCode() != 200) {
            throw new Exception("Failed to fetch PUUID for " + username + "#" + tag);
        }

        JsonObject puuidJson = JsonParser.parseString(puuidResponse.body()).getAsJsonObject();
        return puuidJson.get("puuid").getAsString();
    }

    public static String getTftPuuid(String username, String tag) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String encodedGameName = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String encodedTagLine = URLEncoder.encode(tag, StandardCharsets.UTF_8);

        String puuidUrl = String.format("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s?api_key=%s",
                encodedGameName, encodedTagLine, TFT_API_KEY);

        HttpRequest puuidRequest = HttpRequest.newBuilder()
                .uri(URI.create(puuidUrl))
                .build();
        HttpResponse<String> puuidResponse = client.send(puuidRequest, HttpResponse.BodyHandlers.ofString());

        if (puuidResponse.statusCode() != 200) {
            throw new Exception("Failed to fetch PUUID for " + username + "#" + tag);
        }

        JsonObject puuidJson = JsonParser.parseString(puuidResponse.body()).getAsJsonObject();
        return puuidJson.get("puuid").getAsString();
    }
}
