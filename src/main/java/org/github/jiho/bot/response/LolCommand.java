package org.github.jiho.bot.response;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.github.jiho.bot.Constants.API_KEY;

public class LolCommand extends ListenerAdapter {

    private static final String RIOT_API_KEY = API_KEY; // API 키 입력

    private static final Map<String, String> tierMap = new HashMap<>();
    static {
        tierMap.put("IRON", "아이언");
        tierMap.put("BRONZE", "브론즈");
        tierMap.put("SILVER", "실버");
        tierMap.put("GOLD", "골드");
        tierMap.put("PLATINUM", "플래티넘");
        tierMap.put("DIAMOND", "다이아몬드");
        tierMap.put("MASTER", "마스터");
        tierMap.put("GRANDMASTER", "그랜드마스터");
        tierMap.put("CHALLENGER", "챌린저");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        User user = event.getAuthor();
        EmbedBuilder eb = new EmbedBuilder();


        if (user.isBot()) return;

        if (event.isFromType(ChannelType.TEXT)) {
            TextChannel tc = event.getChannel().asTextChannel();  // 서버 채널
            Message message = event.getMessage();

            if (message.getContentRaw().startsWith("!lol")) {
                String content = message.getContentRaw().substring(4).trim();

                // 메시지가 "!lol"만 입력된 경우 처리
                if (content.isEmpty()) {
                    String username = "여자를뺏겼어요"; // 기본값 설정
                    String tag = "KR1"; // 기본값 설정

                    // 기본 사용자 정보로 티어 정보 가져오기
                    String tierInfo = fetchLolTierInfo(username, tag);
                      eb.setAuthor(" LOL 티어");
                      eb.setTitle(username +"#" + tag + "의 티어");
                      eb.setDescription("<:diamond:1280206153929854986>" + tierInfo);
                      eb.setColor(0X5383E8);
                      eb.setFooter("footer");

//                    message.reply(tierInfo).queue();
                      message.reply(MessageCreateData.fromEmbeds(eb.build())).queue();
                } else {
                    String[] args = content.split(" ");
                    if (args.length == 1 && args[0].contains("#")) {
                        String[] nameAndTag = args[0].split("#", 2); // 두 번째 # 이후의 모든 부분을 태그로 인식
                        if (nameAndTag.length == 2) {
                            String username = nameAndTag[0];
                            String tag = nameAndTag[1].toUpperCase(); // 태그를 대문자로 변환

                            // 입력된 사용자 정보로 티어 정보 가져오기
                            String tierInfo = fetchLolTierInfo(username, tag);
                            eb.setAuthor(username +"#" + tag + "의 티어");
                            eb.setTitle("LOL 티어");
                            eb.setDescription(tierInfo);
                            eb.setColor(0X5383E8);
                            eb.setFooter("footer");

//                    message.reply(tierInfo).queue();
                            message.reply(MessageCreateData.fromEmbeds(eb.build())).queue();
                        }
                    } else {
                        // 잘못된 형식이거나 태그가 없는 경우
                        message.reply("잘못된 형식입니다. 올바른 형식은 `사용자이름#태그`입니다.").queue();
                    }
                }
            }
        }
    }
    private String fetchLolTierInfo(String gameName, String tagLine) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 한글과 특수 문자 인코딩
            String encodedGameName = URLEncoder.encode(gameName, StandardCharsets.UTF_8);
            String encodedTagLine = URLEncoder.encode(tagLine, StandardCharsets.UTF_8);

            // 1. PUUID 가져오기
            String puuidUrl = String.format("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s?api_key=%s",
                    encodedGameName, encodedTagLine, RIOT_API_KEY);

            HttpRequest puuidRequest = HttpRequest.newBuilder()
                    .uri(URI.create(puuidUrl))
                    .build();
            HttpResponse<String> puuidResponse = client.send(puuidRequest, HttpResponse.BodyHandlers.ofString());

            if (puuidResponse.statusCode() != 200) {
                return "Failed to fetch PUUID for " + gameName + "#" + tagLine;
            }

            JsonObject puuidJson = JsonParser.parseString(puuidResponse.body()).getAsJsonObject();
            String puuid = puuidJson.get("puuid").getAsString();

            // 2. Summoner ID로 티어와 점수 가져오기
            String summonerUrl = String.format("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/%s?api_key=%s",
                    puuid, RIOT_API_KEY);
            HttpRequest summonerRequest = HttpRequest.newBuilder()
                    .uri(URI.create(summonerUrl))
                    .build();
            HttpResponse<String> summonerResponse = client.send(summonerRequest, HttpResponse.BodyHandlers.ofString());

            if (summonerResponse.statusCode() != 200) {
                return "Failed to fetch Summoner ID for " + gameName + "#" + tagLine;
            }

            JsonObject summonerJson = JsonParser.parseString(summonerResponse.body()).getAsJsonObject();
            String summonerId = summonerJson.get("id").getAsString();

            // 3. 티어와 점수 정보 가져오기
            String leagueUrl = String.format("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/%s?api_key=%s",
                    summonerId, RIOT_API_KEY);
            HttpRequest leagueRequest = HttpRequest.newBuilder()
                    .uri(URI.create(leagueUrl))
                    .build();
            HttpResponse<String> leagueResponse = client.send(leagueRequest, HttpResponse.BodyHandlers.ofString());

            if (leagueResponse.statusCode() != 200) {
                return "Failed to fetch League info for " + gameName + "#" + tagLine;
            }

            JsonArray leagueArray = JsonParser.parseString(leagueResponse.body()).getAsJsonArray();
            JsonObject rankedInfo = null;

            for (JsonElement element : leagueArray) {
                JsonObject info = element.getAsJsonObject();
                if ("RANKED_SOLO_5x5".equals(info.get("queueType").getAsString())) {
                    rankedInfo = info;
                    break;
                }
            }

            if (rankedInfo == null) {
                return gameName + "#" + tagLine + " has no ranked solo games.";
            }

            String tier = rankedInfo.get("tier").getAsString();
            String rank = rankedInfo.get("rank").getAsString();
            int leaguePoints = rankedInfo.get("leaguePoints").getAsInt();
            String translatedTier = tierMap.getOrDefault(tier, tier);

            // 4. 최근 매치 정보 가져오기 (연승/연패 계산)
            String matchListUrl = String.format("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=10&api_key=%s",
                    puuid, RIOT_API_KEY);
            HttpRequest matchListRequest = HttpRequest.newBuilder()
                    .uri(URI.create(matchListUrl))
                    .build();
            HttpResponse<String> matchListResponse = client.send(matchListRequest, HttpResponse.BodyHandlers.ofString());

            if (matchListResponse.statusCode() != 200) {
                return "Failed to fetch match list for " + gameName + "#" + tagLine;
            }

            JsonArray matchIds = JsonParser.parseString(matchListResponse.body()).getAsJsonArray();

            int winStreak = 0;
            int loseStreak = 0;

            for (JsonElement matchIdElement : matchIds) {
                String matchId = matchIdElement.getAsString();
                String matchUrl = String.format("https://asia.api.riotgames.com/lol/match/v5/matches/%s?api_key=%s",
                        matchId, RIOT_API_KEY);
                HttpRequest matchRequest = HttpRequest.newBuilder()
                        .uri(URI.create(matchUrl))
                        .build();
                HttpResponse<String> matchResponse = client.send(matchRequest, HttpResponse.BodyHandlers.ofString());

                if (matchResponse.statusCode() != 200) {
                    // API 호출 실패에 대한 구체적인 오류 메시지를 추가
                    return String.format("Failed to fetch match data for match ID %s: %d", matchId, matchResponse.statusCode());
                }

                JsonObject matchJson = JsonParser.parseString(matchResponse.body()).getAsJsonObject();
                JsonArray participants = matchJson.getAsJsonObject("info").getAsJsonArray("participants");

                boolean win = false;
                for (JsonElement participantElement : participants) {
                    JsonObject participant = participantElement.getAsJsonObject();
                    String participantPuuid = participant.get("puuid").getAsString();

                    if (participantPuuid.equals(puuid)) {
                        win = participant.get("win").getAsBoolean();
                        break;
                    }
                }

                // 승패 결과에 따른 연승/연패 처리
                if (win) {
                    if(loseStreak > 0){
                        break;
                    }
                    winStreak++;
                }
                else {
                    if(winStreak > 0){
                        break;
                    }
                    loseStreak++;
                }
            }

// 연승/연패 정보를 출력할 메시지 작성
            String streak = winStreak > 0 ? winStreak + "연승중" : loseStreak + "연패중";

            return String.format("%s %s %s - %d LP\n%s", gameName, translatedTier, rank, leaguePoints, streak);

        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while fetching data for " + gameName + "#" + tagLine;
        }
    }
}
