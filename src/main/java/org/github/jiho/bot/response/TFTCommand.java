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
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.github.jiho.bot.util.PuuIdUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.github.jiho.bot.Constants.API_KEY;
import static org.github.jiho.bot.Constants.TFT_KEY;

public class TFTCommand extends ListenerAdapter {

    private static final String RIOT_API_KEY = TFT_KEY; // API 키 입력
    private static final String ID_API_KEY = API_KEY;

    private static final Map<String, String> tierMap = new HashMap<>();

    static {
        tierMap.put("IRON", "아이언");
        tierMap.put("BRONZE", "브론즈");
        tierMap.put("SILVER", "실버");
        tierMap.put("GOLD", "골드");
        tierMap.put("PLATINUM", "플래티넘");
        tierMap.put("EMERALD", "에메랄드");
        tierMap.put("DIAMOND", "다이아몬드");
        tierMap.put("MASTER", "마스터");
        tierMap.put("GRANDMASTER", "그랜드마스터");
        tierMap.put("CHALLENGER", "챌린저");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        User user = event.getAuthor();

        if (user.isBot()) return;

        if (event.isFromType(ChannelType.TEXT)) {
            TextChannel tc = event.getChannel().asTextChannel();  // 서버 채널
            Message message = event.getMessage();

            if (message.getContentRaw().startsWith("!tft")) {
                String content = message.getContentRaw().substring(4).trim();
                EmbedBuilder eb = new EmbedBuilder();

                try {
                    String username, tag;

                    if (content.isEmpty()) {
                        // 기본 사용자 정보
                        username = "낭낭만"; // 기본 사용자 이름
                        tag = "KR1"; // 기본 태그
                    } else {
                        String[] args = content.split(" ");
                        if (args.length == 1 && args[0].contains("#")) {
                            String[] nameAndTag = args[0].split("#", 2);
                            if (nameAndTag.length == 2) {
                                username = nameAndTag[0];
                                tag = nameAndTag[1].toUpperCase();
                            } else {
                                throw new IllegalArgumentException("잘못된 형식입니다. 올바른 형식은 `사용자이름#태그`입니다.");
                            }
                        } else {
                            throw new IllegalArgumentException("잘못된 형식입니다. 올바른 형식은 `사용자이름#태그`입니다.");
                        }
                    }

                    // PUUID 가져오기
                    String puuid = PuuIdUtil.getTftPuuid(username, tag);

                    // Summoner ID 가져오기
                    String summonerId = getTftSummonerIdByPuuid(puuid);

                    // TFT 랭크 정보 가져오기
                    String tftRankInfo = getTftRankInfo(summonerId);

                    // 최근 10매치 정보 가져오기
                    String placementsInfo = getTftPlacements(puuid);

                    String tierEmote = getTierEmote(tftRankInfo);

                    // 임베드 설정
                    eb.setAuthor(username + "#" + tag + "의 TFT 정보");
                    eb.setTitle("TeamFight Tactics");
                    eb.setDescription(tierEmote + " " + tftRankInfo + "\n" + placementsInfo);

                    eb.setFooter("Riot Games API를 통해 제공된 데이터");

                    // 임베드 메시지 전송
                    message.reply(MessageCreateData.fromEmbeds(eb.build())).queue();

                } catch (IllegalArgumentException e) {
                    message.reply(e.getMessage()).queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    message.reply("데이터를 불러오는 중 오류가 발생했습니다.").queue();
                }
            }
        }
    }

    private String getTierEmote(String tierInfo) {
        if (tierInfo.contains("아이언")) {
            return "<:iron:1280326738471026790>";
        } else if (tierInfo.contains("브론즈")) {
            return "<:bronze:1280326650751483956>";
        } else if (tierInfo.contains("실버")) {
            return "<:silver:1280326671810953328>";
        } else if (tierInfo.contains("골드")) {
            return "<:gold:1280326687963218025>";
        } else if (tierInfo.contains("플래티넘")) {
            return "<:platinum:1280326699648680058>";
        } else if (tierInfo.contains("에메랄드")) {
            return "<:emerald:1280326723421863938>";
        } else if (tierInfo.contains("다이아몬드")) {
            return "<:diamond:1280326941710225469>";
        } else if (tierInfo.contains("마스터")) {
            return "<:master:1280326798386925691>";
        } else if (tierInfo.contains("그랜드마스터")) {
            return "<:grandmaster:1280326812010020874>";
        } else if (tierInfo.contains("챌린저")) {
            return "<:challenger:1280326825486319701>";
        } else {
            return "";
        }
    }


    private String getTftSummonerIdByPuuid(String puuid) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String summonerUrl = String.format("https://kr.api.riotgames.com/tft/summoner/v1/summoners/by-puuid/%s?api_key=%s",
                puuid, RIOT_API_KEY);
        HttpRequest summonerRequest = HttpRequest.newBuilder()
                .uri(URI.create(summonerUrl))
                .build();
        HttpResponse<String> summonerResponse = client.send(summonerRequest, HttpResponse.BodyHandlers.ofString());

        if (summonerResponse.statusCode() != 200) {
            // 오류 디버깅을 위해 상태 코드와 응답 본문을 출력
            System.err.println("Failed to fetch Summoner ID for PUUID: " + puuid);
            System.err.println("Response Code: " + summonerResponse.statusCode());
            System.err.println("Response Body: " + summonerResponse.body());
            throw new Exception("Failed to fetch Summoner ID for PUUID: " + puuid);
        }

        JsonObject summonerJson = JsonParser.parseString(summonerResponse.body()).getAsJsonObject();
        return summonerJson.get("id").getAsString();
    }

    private String getTftRankInfo(String summonerId) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String tftRankUrl = String.format("https://kr.api.riotgames.com/tft/league/v1/entries/by-summoner/%s?api_key=%s",
                summonerId, RIOT_API_KEY);
        HttpRequest tftRankRequest = HttpRequest.newBuilder()
                .uri(URI.create(tftRankUrl))
                .build();
        HttpResponse<String> tftRankResponse = client.send(tftRankRequest, HttpResponse.BodyHandlers.ofString());

        if (tftRankResponse.statusCode() != 200) {
            // 오류 디버깅을 위해 상태 코드와 응답 본문을 출력
            System.err.println("Failed to fetch TFT rank info for Summoner ID: " + summonerId);
            System.err.println("Response Code: " + tftRankResponse.statusCode());
            System.err.println("Response Body: " + tftRankResponse.body());
            throw new Exception("Failed to fetch TFT rank info for Summoner ID: " + summonerId);
        }

        JsonArray tftRankArray = JsonParser.parseString(tftRankResponse.body()).getAsJsonArray();

        if (tftRankArray.size() == 0) {
            return "랭크 정보가 없습니다.";
        }

        JsonObject rankedInfo = null;

        for (JsonElement element : tftRankArray) {
            JsonObject info = element.getAsJsonObject();
            if ("RANKED_TFT".equals(info.get("queueType").getAsString())) {
                rankedInfo = info;
                break;
            }
        }

        if (rankedInfo == null) {
            return "랭크 정보가 없습니다.";
        }

        String tier = rankedInfo.get("tier").getAsString();
        String rank = rankedInfo.get("rank").getAsString();
        int leaguePoints = rankedInfo.get("leaguePoints").getAsInt();
        String translatedTier = tierMap.getOrDefault(tier, tier);

        return String.format("%s %s - %d LP", translatedTier, rank, leaguePoints);
    }

    private String getTftPlacements(String puuid) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String matchListUrl = String.format(
                "https://asia.api.riotgames.com/tft/match/v1/matches/by-puuid/%s/ids?start=0&count=20&api_key=%s",
                puuid, RIOT_API_KEY);
        HttpRequest matchListRequest = HttpRequest.newBuilder()
                .uri(URI.create(matchListUrl))
                .build();
        HttpResponse<String> matchListResponse = client.send(matchListRequest, HttpResponse.BodyHandlers.ofString());

        if (matchListResponse.statusCode() != 200) {
            throw new Exception("Failed to fetch match list for PUUID: " + puuid);
        }

        JsonArray matchIds = JsonParser.parseString(matchListResponse.body()).getAsJsonArray();

        if (matchIds.size() == 0) {
            return "최근 기록이 없습니다.";
        }

        StringBuilder placements = new StringBuilder("최근 경기 등수: ");
        int placementSum = 0;
        int validMatches = 0;

        for (JsonElement matchIdElement : matchIds) {
            String matchId = matchIdElement.getAsString();
            String matchUrl = String.format("https://asia.api.riotgames.com/tft/match/v1/matches/%s?api_key=%s",
                    matchId, RIOT_API_KEY);
            HttpRequest matchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(matchUrl))
                    .build();
            HttpResponse<String> matchResponse = client.send(matchRequest, HttpResponse.BodyHandlers.ofString());

            if (matchResponse.statusCode() != 200) {
                throw new Exception(String.format("Failed to fetch match data for match ID %s: %d", matchId, matchResponse.statusCode()));
            }

            JsonObject matchJson = JsonParser.parseString(matchResponse.body()).getAsJsonObject();
            JsonObject info = matchJson.getAsJsonObject("info");

            // 필터: queueId가 1100이고 tft_game_type이 standard인 경기만 포함
            if (info.has("queueId") && info.get("queueId").getAsInt() == 1100 &&
                    info.has("tft_game_type") && "standard".equals(info.get("tft_game_type").getAsString())) {
                JsonArray participants = info.getAsJsonArray("participants");

                for (JsonElement participantElement : participants) {
                    JsonObject participant = participantElement.getAsJsonObject();
                    String participantPuuid = participant.get("puuid").getAsString();

                    if (participantPuuid.equals(puuid)) {
                        if (participant.has("placement")) {
                            int placement = participant.get("placement").getAsInt();
                            placementSum += placement;
                            placements.append(placement).append(" ");
                            validMatches++;
                        }
                        break;
                    }
                }
            }

            // 10개의 유효한 랭크 게임이 수집되면 루프 종료
            if (validMatches >= 10) {
                break;
            }
        }

        if (validMatches == 0) {
            return "최근 기록이 없습니다.";
        }

        double averagePlacement = (double) placementSum / validMatches;

        return placements.toString().trim() + "\n평균: [" + String.format("%.1f", averagePlacement) + "]등";
    }


}