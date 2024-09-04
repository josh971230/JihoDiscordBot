package org.github.jiho.bot.response;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.github.jiho.bot.util.ValTierEmoteUtil;

public class ValRankNotifier {
    private final JDA jda;
    private final String region;
    private final String username;
    private final String tag;
    private final String channelId;
    private String lastRankInfo = null;  // 초기 값은 null

    public ValRankNotifier(JDA jda, String region, String username, String tag, String channelId) {
        this.jda = jda;
        this.region = region;
        this.username = username;
        this.tag = tag;
        this.channelId = channelId;
    }

    public void start() {
        System.out.println("Starting ValRankNotifier...");
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkAndUpdateRank();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 60000); // 2분(120000ms)마다 실행
    }

    private void checkAndUpdateRank() {
        System.out.println("Checking and updating rank...");
        String url = String.format("https://splendid-groovy-feverfew.glitch.me/valorant/%s/%s/%s?onlyRank=true&mmrChange=true", region, username, tag);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Received response with status code: " + response.statusCode());

            if (response.statusCode() == 200) {
                String currentRankInfo = response.body().trim();
                System.out.println("Current rank info: " + currentRankInfo);

                // "Either you entered an incorrect username..." 메시지를 무시
                if (currentRankInfo.contains("Either you entered an incorrect username")) {
                    System.out.println("Invalid username or backend error detected. Ignoring update.");
                    return;  // 이 경우 업데이트 하지 않음
                }

                // lastRankInfo가 null인 경우(첫 실행 시) 업데이트를 하지 않음
                if (lastRankInfo == null) {
                    System.out.println("First run, initializing lastRankInfo without sending notification.");
                    lastRankInfo = currentRankInfo;  // 랭크 정보를 업데이트하지만 알림은 보내지 않음
                    return;
                }

                if (!currentRankInfo.equals(lastRankInfo)) {
                    System.out.println("Rank has changed! Sending update...");
                    sendRankUpdate(currentRankInfo);
                    lastRankInfo = currentRankInfo;
                } else {
                    System.out.println("Rank has not changed.");
                }
            } else {
                System.err.println("Failed to fetch rank info, response code: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Exception occurred during rank check: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static final Map<String, String> tierMap = new HashMap<>();

    static {
        tierMap.put("Iron", "아이언");
        tierMap.put("Bronze", "브론즈");
        tierMap.put("Silver", "실버");
        tierMap.put("Gold", "골드");
        tierMap.put("Platinum", "플래티넘");
        tierMap.put("Diamond", "다이아몬드");
        tierMap.put("Ascendant", "초월자");
        tierMap.put("Immortal", "불멸자");
        tierMap.put("Radiant", "래디언트");
    }

    private void sendRankUpdate(String rankInfo) {
        System.out.println("Sending rank update...");
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String emoji = ValTierEmoteUtil.getTierEmote(rankInfo);

            String[] rankParts = rankInfo.split(" ");
            String tierName = rankParts[0];
            String translatedTier = tierMap.getOrDefault(tierName, tierName);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(username +  "의 발로란트 속보");
            eb.setTitle("Valorant");
            eb.setDescription(emoji + " " + translatedTier + " " + String.join(" ", rankParts).replaceFirst(tierName, ""));

            // RR 변동에 따라 색상 설정
            if (rankInfo.contains("[-") || rankInfo.contains("강등")) {
                eb.setColor(0xE84057); // 빨간색
                System.out.println("Rank demoted or negative RR, setting embed color to red.");
            } else {
                eb.setColor(0x5383E8); // 기본 색상 (파란색)
                System.out.println("Setting embed color to blue.");
            }

            // 오류 메시지 포함시 메시지 전송하지 않도록 필터링
            if (!rankInfo.contains("Either you entered an incorrect username")) {
                channel.sendMessageEmbeds(eb.build()).queue();
                System.out.println("Rank update sent to channel ID: " + channelId);
            } else {
                System.out.println("Skipping rank update due to invalid username or backend error.");
            }
        } else {
            System.err.println("Channel with ID " + channelId + " not found.");
        }
    }
}
