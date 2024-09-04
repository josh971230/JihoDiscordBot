package org.github.jiho.bot.response;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.github.jiho.bot.util.PuuIdUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Timer;
import java.util.TimerTask;

import static org.github.jiho.bot.Constants.TFT_KEY;

public class TftRankNotifier {

    private final JDA jda;
    private final String channelId;
    private final String puuid;
    private String lastMatchId = ""; // 마지막 게임 ID를 저장

    public TftRankNotifier(JDA jda, String puuid, String channelId) {
        this.jda = jda;
        this.puuid = puuid;
        this.channelId = channelId;
    }

    public void start(long interval) {
        System.out.println("Starting TftRankNotifier with interval: " + interval);

        initializeLastMatchId();

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkAndNotify();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, interval, interval);
    }

    private void initializeLastMatchId() {
        try {
            lastMatchId = getLastMatchId(puuid);
            System.out.println("Initialized last match ID: " + lastMatchId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAndNotify() throws Exception {
        System.out.println("Checking TFT rank and notifying...");

        String matchId = getLastMatchId(puuid);
        System.out.println("Fetched Last Match ID: " + matchId);

        if (!matchId.equals(lastMatchId)) {
            lastMatchId = matchId;
            JsonObject matchInfo = getMatchInfo(matchId, puuid);
            System.out.println("Fetched Match Info: " + matchInfo);

            sendNotification(matchInfo);
        }
    }

    private String getLastMatchId(String puuid) throws Exception {
        System.out.println("Fetching last match ID for PUUID: " + puuid);

        HttpClient client = HttpClient.newHttpClient();
        String matchListUrl = String.format("https://asia.api.riotgames.com/tft/match/v1/matches/by-puuid/%s/ids?start=0&count=1&api_key=%s", puuid, TFT_KEY);
        HttpRequest matchListRequest = HttpRequest.newBuilder().uri(URI.create(matchListUrl)).build();
        HttpResponse<String> matchListResponse = client.send(matchListRequest, HttpResponse.BodyHandlers.ofString());

        if (matchListResponse.statusCode() != 200) {
            System.err.println("Failed to fetch match list for PUUID: " + puuid + ", Status Code: " + matchListResponse.statusCode());
            throw new Exception("Failed to fetch match list");
        }

        String responseBody = matchListResponse.body();
        System.out.println("Match list response: " + responseBody);

        JsonArray matchIds = JsonParser.parseString(responseBody).getAsJsonArray();

        if (matchIds.size() == 0) {
            System.err.println("No matches found for PUUID: " + puuid);
            throw new Exception("No matches found");
        }

        String lastMatchId = matchIds.get(0).getAsString();
        System.out.println("Last match ID: " + lastMatchId);

        return lastMatchId;
    }

    private JsonObject getMatchInfo(String matchId, String puuid) throws Exception {
        System.out.println("Fetching match info for match ID: " + matchId);
        HttpClient client = HttpClient.newHttpClient();
        String matchUrl = String.format("https://asia.api.riotgames.com/tft/match/v1/matches/%s?api_key=%s", matchId, TFT_KEY);
        HttpRequest matchRequest = HttpRequest.newBuilder().uri(URI.create(matchUrl)).build();
        HttpResponse<String> matchResponse = client.send(matchRequest, HttpResponse.BodyHandlers.ofString());

        if (matchResponse.statusCode() != 200) {
            throw new Exception("Failed to fetch match info");
        }

        JsonObject matchJson = JsonParser.parseString(matchResponse.body()).getAsJsonObject();
        JsonArray participants = matchJson.getAsJsonObject("info").getAsJsonArray("participants");

        for (JsonElement participantElement : participants) {
            JsonObject participant = participantElement.getAsJsonObject();
            if (participant.get("puuid").getAsString().equals(puuid)) {
                return participant; // 이 플레이어의 경기 정보를 반환
            }
        }
        return null;
    }

    private void sendNotification(JsonObject matchInfo) {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel != null && matchInfo != null) {
            int placement = matchInfo.get("placement").getAsInt();
            String placementMessage = "순위: " + placement + "등";

            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor("김필벌레녀석의 롤체속보");
            eb.setTitle("TeamFightTactics");
            eb.setDescription(placementMessage);

            if(placement == 1){
                eb.setColor(0X10b186);
            }else if(placement > 2 && placement < 5){
                eb.setColor(0X2078c5);
            }else eb.setColor(0X858999);

            eb.setFooter("Riot Games API를 통해 제공된 데이터");

            channel.sendMessageEmbeds(eb.build()).queue();
        }
    }
}