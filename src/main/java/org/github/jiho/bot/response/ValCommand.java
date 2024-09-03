package org.github.jiho.bot.response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ValCommand extends ListenerAdapter {
    private static final Map < String, String > tierMap = new HashMap();

    public void onMessageReceived(MessageReceivedEvent event) {
        User user = event.getAuthor();
        if (!user.isBot()) {
            if (event.isFromType(ChannelType.TEXT)) {
                TextChannel tc = event.getChannel().asTextChannel();
                Message message = event.getMessage();
                if (message.getContentRaw().startsWith("!val")) {
                    String[] args = message.getContentRaw().substring(1).split(" ");
                    if (args.length > 2 || args.length == 2 && !args[1].contains("#")) {
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
                    message.reply(tierInfo).queue();
                }
            }

        }
    }

    private String getValorantTierAndRank(String region, String username, String tag) {
        String url = String.format("https://splendid-groovy-feverfew.glitch.me/valorant/%s/%s/%s?onlyRank=true&mmrChange=true", region, username, tag);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse < String > response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String rankInfo = ((String) response.body()).trim();
                String[] parts = rankInfo.split(" ", 2);
                String tier = parts[0];
                String restOfInfo = parts.length > 1 ? parts[1] : "";
                String translatedTier = (String) tierMap.getOrDefault(tier, tier);
                return translatedTier + " " + restOfInfo;
            } else {
                return "Failed to fetch data for player " + username + "#" + tag;
            }
        } catch (Exception var13) {
            var13.printStackTrace();
            return "Error occurred while fetching data for player " + username + "#" + tag;
        }
    }

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
}
