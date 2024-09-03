package org.github.jiho.bot.response;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
        }, 0, 120000); // 2분(120000ms)마다 실행
    }

    private void checkAndUpdateRank() {
        String url = String.format("https://splendid-groovy-feverfew.glitch.me/valorant/%s/%s/%s?onlyRank=true&mmrChange=true", region, username, tag);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String currentRankInfo = response.body().trim();

                // lastRankInfo가 null인 경우(첫 실행 시) 업데이트를 하지 않음
                if (lastRankInfo == null) {
                    lastRankInfo = currentRankInfo;  // 랭크 정보를 업데이트하지만 알림은 보내지 않음
                    return;
                }

                if (!currentRankInfo.equals(lastRankInfo)) {
                    sendRankUpdate(currentRankInfo);
                    lastRankInfo = currentRankInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendRankUpdate(String rankInfo) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String emoji = ValTierEmoteUtil.getTierEmote(rankInfo);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(username + "#" + tag + "의 발로란트 랭크 정보");
            eb.setTitle("Valorant");
            eb.setDescription(emoji + " " + rankInfo);

            // RR 변동에 따라 색상 설정
            if (rankInfo.contains("[+") || rankInfo.contains("승격")) {
                eb.setColor(0x00FF00); // 초록색
            } else if (rankInfo.contains("[-") || rankInfo.contains("강등")) {
                eb.setColor(0xFF0000); // 빨간색
            } else {
                eb.setColor(0xFFFFFF); // 기본 색상 (흰색)
            }

            channel.sendMessageEmbeds(eb.build()).queue();
        }
    }
}
