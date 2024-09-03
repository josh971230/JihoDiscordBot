package org.github.jiho.bot.response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import org.github.jiho.bot.util.ValTierEmoteUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ValCommand extends ListenerAdapter {

    private static final String UNKNOWN_TIER_MESSAGE = "랭크 정보가 없습니다.";
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

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        User user = event.getAuthor();
        if (!user.isBot()) {
            if (event.isFromType(ChannelType.TEXT)) {
                TextChannel tc = event.getChannel().asTextChannel();
                Message message = event.getMessage();

                if (message.getContentRaw().startsWith("!val")) {
                    String[] args = message.getContentRaw().substring(1).split(" ");
                    if (args.length > 2 || (args.length == 2 && !args[1].contains("#"))) {
                        return;
                    }

                    String region = "kr";
                    String username = "단비우";
                    String tag = "0411";
                    if (args.length == 2) {
                        String[] nameAndTag = args[1].split("#");
                        if (nameAndTag.length != 2) {
                            return;
                        }

                        username = nameAndTag[0];
                        tag = nameAndTag[1];
                    }

                    String tierInfo = this.getValorantTierAndRank(region, username, tag);
                    EmbedBuilder eb = new EmbedBuilder();

                    // RR 값이 양수인지 음수인지에 따라 색상 변경
                    if (tierInfo.contains("[-")) {
                        eb.setColor(0xE84057); // 빨간색 (음수)
                    }  else {
                        eb.setColor(0x5383E8); // 기본 색상 (흰색)
                    }

                    eb.setAuthor(username + "#" + tag + "의 발로란트 랭크 정보");
                    eb.setTitle("Valorant");
                    eb.setDescription(tierInfo);
                    eb.setFooter("Valorant API를 통해 제공된 데이터");
                    message.replyEmbeds(eb.build()).queue();
                }
            }
        }
    }

    private String getValorantTierAndRank(String region, String username, String tag) {
        String url = String.format("https://splendid-groovy-feverfew.glitch.me/valorant/%s/%s/%s?onlyRank=true&mmrChange=true", region, username, tag);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String rankInfo = response.body().trim();
                String emoji = ValTierEmoteUtil.getTierEmote(rankInfo);
                String translatedRankInfo = translateRankInfo(rankInfo);
                return emoji + " " + translatedRankInfo;
            } else {
                return ValTierEmoteUtil.getTierEmote("") + " " + UNKNOWN_TIER_MESSAGE;
            }
        } catch (Exception var13) {
            var13.printStackTrace();
            return ValTierEmoteUtil.getTierEmote("") + " " + "플레이어 정보를 가져오는 중 오류가 발생했습니다: " + username + "#" + tag;
        }
    }

    private String translateRankInfo(String rankInfo) {
        for (Map.Entry<String, String> entry : tierMap.entrySet()) {
            if (rankInfo.contains(entry.getKey())) {
                return rankInfo.replace(entry.getKey(), entry.getValue());
            }
        }
        return rankInfo;
    }
}
