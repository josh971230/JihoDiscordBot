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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.github.jiho.bot.Constants.API_KEY;

public class LolRankNotifier {

    private final JDA jda;
    private final String channelId;
    private final String username;
    private final String tag;
    private int previousLp = -1; // 초기 LP 값을 설정하지 않음
    private String lastMatchId = ""; // 마지막 게임 ID를 저장

    public LolRankNotifier(JDA jda, String channelId, String username, String tag) {
        this.jda = jda;
        this.channelId = channelId;
        this.username = username;
        this.tag = tag;
    }

    public void start(long interval) {
        System.out.println("Starting LolRankNotifier with interval: " + interval);
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
        }, 0, interval);
    }

    private void checkAndNotify() throws Exception {
        System.out.println("Checking rank and notifying...");

        String puuid = PuuIdUtil.getPuuid(username, tag);
        System.out.println("Fetched PUUID: " + puuid);

        String summonerId = getSummonerIdByPuuid(puuid);
        System.out.println("Fetched Summoner ID: " + summonerId);

        JsonObject rankedInfo = getRankedInfoBySummonerId(summonerId);
        System.out.println("Fetched Ranked Info: " + rankedInfo);

        if (rankedInfo != null) {
            int currentLp = rankedInfo.get("leaguePoints").getAsInt();
            String tier = rankedInfo.get("tier").getAsString();
            String rank = rankedInfo.get("rank").getAsString();

            System.out.println("Current LP: " + currentLp + ", Tier: " + tier + ", Rank: " + rank);

            // LP 변동 체크
            if (previousLp != -1 && currentLp != previousLp) {
                String matchId = getLastMatchId(puuid);

                System.out.println("Fetched Last Match ID: " + matchId);

                if (!matchId.equals(lastMatchId)) {
                    lastMatchId = matchId;
                    JsonObject matchInfo = getMatchInfo(matchId, puuid);
                    System.out.println("Fetched Match Info: " + matchInfo);

                    sendNotification(tier, rank, currentLp, matchInfo);
                }
            }
            previousLp = currentLp;
        }else
            System.out.println("No ranked info available.");
    }

    private String getSummonerIdByPuuid(String puuid) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String summonerUrl = String.format("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/%s?api_key=%s", puuid, API_KEY);
        HttpRequest summonerRequest = HttpRequest.newBuilder().uri(URI.create(summonerUrl)).build();
        HttpResponse<String> summonerResponse = client.send(summonerRequest, HttpResponse.BodyHandlers.ofString());

        if (summonerResponse.statusCode() != 200) {
            throw new Exception("Failed to fetch Summoner ID");
        }

        JsonObject summonerJson = JsonParser.parseString(summonerResponse.body()).getAsJsonObject();
        return summonerJson.get("id").getAsString();
    }

    private JsonObject getRankedInfoBySummonerId(String summonerId) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String leagueUrl = String.format("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/%s?api_key=%s", summonerId, API_KEY);
        HttpRequest leagueRequest = HttpRequest.newBuilder().uri(URI.create(leagueUrl)).build();
        HttpResponse<String> leagueResponse = client.send(leagueRequest, HttpResponse.BodyHandlers.ofString());

        if (leagueResponse.statusCode() != 200) {
            throw new Exception("Failed to fetch ranked info");
        }

        JsonArray leagueArray = JsonParser.parseString(leagueResponse.body()).getAsJsonArray();

        for (JsonElement element : leagueArray) {
            JsonObject info = element.getAsJsonObject();
            if ("RANKED_SOLO_5x5".equals(info.get("queueType").getAsString())) {
                return info;
            }
        }
        return null;
    }

    private String getLastMatchId(String puuid) throws Exception {
        System.out.println("Fetching last match ID for PUUID: " + puuid);  // 디버깅 로그 추가

        HttpClient client = HttpClient.newHttpClient();
        String matchListUrl = String.format("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=1&api_key=%s", puuid, API_KEY);
        HttpRequest matchListRequest = HttpRequest.newBuilder().uri(URI.create(matchListUrl)).build();
        HttpResponse<String> matchListResponse = client.send(matchListRequest, HttpResponse.BodyHandlers.ofString());

        if (matchListResponse.statusCode() != 200) {
            System.err.println("Failed to fetch match list for PUUID: " + puuid + ", Status Code: " + matchListResponse.statusCode());  // 디버깅 로그 추가
            throw new Exception("Failed to fetch match list");
        }

        String responseBody = matchListResponse.body();
        System.out.println("Match list response: " + responseBody);  // 디버깅 로그 추가

        JsonArray matchIds = JsonParser.parseString(responseBody).getAsJsonArray();

        if (matchIds.size() == 0) {
            System.err.println("No matches found for PUUID: " + puuid);  // 디버깅 로그 추가
            throw new Exception("No matches found");
        }

        String lastMatchId = matchIds.get(0).getAsString();
        System.out.println("Last match ID: " + lastMatchId);  // 디버깅 로그 추가

        return lastMatchId;
    }

    private JsonObject getMatchInfo(String matchId, String puuid) throws Exception {
        System.out.println("Fetching match info for match ID: " + matchId);
        HttpClient client = HttpClient.newHttpClient();
        String matchUrl = String.format("https://asia.api.riotgames.com/lol/match/v5/matches/%s?api_key=%s", matchId, API_KEY);
        HttpRequest matchRequest = HttpRequest.newBuilder().uri(URI.create(matchUrl)).build();
        HttpResponse<String> matchResponse = client.send(matchRequest, HttpResponse.BodyHandlers.ofString());

        if (matchResponse.statusCode() != 200) {
            throw new Exception("Failed to fetch match info");
        }

        JsonObject matchJson = JsonParser.parseString(matchResponse.body()).getAsJsonObject();
        JsonArray participants = matchJson.getAsJsonObject("info").getAsJsonArray("participants");

        JsonObject participantInfo = null;
        int participantDamage = 0;
        int participantTeamId = 0;
        List<Integer> damageList = new ArrayList<>();

        // 참가자의 팀 ID를 먼저 확인
        for (JsonElement participantElement : participants) {
            JsonObject participant = participantElement.getAsJsonObject();
            if (participant.get("puuid").getAsString().equals(puuid)) {
                participantTeamId = participant.get("teamId").getAsInt();
                participantInfo = participant;
                participantDamage = participant.get("totalDamageDealtToChampions").getAsInt();
                break;
            }
        }

        // 같은 팀의 참가자들 간의 딜량을 비교
        for (JsonElement participantElement : participants) {
            JsonObject participant = participantElement.getAsJsonObject();
            if (participant.get("teamId").getAsInt() == participantTeamId) {
                int damage = participant.get("totalDamageDealtToChampions").getAsInt();
                damageList.add(damage);
            }
        }

        if (participantInfo != null) {
            // 팀 내 피해량 순위를 계산
            int damageRank = 1;
            for (int damage : damageList) {
                if (damage > participantDamage) {
                    damageRank++;
                }
            }
            participantInfo.addProperty("damageRank", damageRank); // 순위를 추가하여 반환
        }

        return participantInfo;
    }

    private void sendNotification(String tier, String rank, int currentLp, JsonObject matchInfo) {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel != null && matchInfo != null) {
            String winStatus = matchInfo.get("win").getAsBoolean() ? "승리" : "패배";
            String kda = String.format("%d/%d/%d", matchInfo.get("kills").getAsInt(), matchInfo.get("deaths").getAsInt(), matchInfo.get("assists").getAsInt());
            int damageRank = matchInfo.get("damageRank").getAsInt();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor( username + "1의 최신전적");
            eb.setTitle("League of Legends");
            eb.setDescription(String.format("%s %s - %d LP\n전적: %s  K/D/A: %s  팀 내 딜량 순위: %d등",
                    tier, rank, currentLp, winStatus, kda, damageRank));

            if (currentLp > previousLp) {
                eb.setColor(0x5383E8); // 파란색 (LP 상승)
            } else {
                eb.setColor(0xE84057); // 빨간색 (LP 하락)
            }

            eb.setFooter("Riot Games API를 통해 제공된 데이터");

            channel.sendMessageEmbeds(eb.build()).queue();
        }
    }

}
