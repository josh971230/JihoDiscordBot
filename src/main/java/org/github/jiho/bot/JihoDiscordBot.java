package org.github.jiho.bot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.github.jiho.bot.response.ValCommand;
import org.github.jiho.bot.response.LolCommand;
import org.github.jiho.bot.response.TFTCommand;
import org.github.jiho.bot.response.ValRankNotifier;
import org.github.jiho.bot.response.LolRankNotifier;

import static org.github.jiho.bot.Constants.BOT_TOKEN;

public class JihoDiscordBot {
    public static void main(String[] args) {
        try {
            JDABuilder jdaBuilder = JDABuilder.createDefault(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setActivity(Activity.playing("박지호 감시"))
                    .addEventListeners(new ValCommand())
                    .addEventListeners(new LolCommand())
                    .addEventListeners(new TFTCommand());

            // JDA 인스턴스 빌드 및 초기화
            var jda = jdaBuilder.build();
            jda.awaitReady();

            // ValRankNotifier 초기화 (발로란트)
            String valRegion = "kr";
            String valUsername = "단비우";
            String valTag = "0411";
            String valChannelId = "325688449652883458"; // 발로란트 알림을 보낼 채널 ID
            ValRankNotifier valNotifier = new ValRankNotifier(jda, valRegion, valUsername, valTag, valChannelId);
            valNotifier.start(); // 발로란트 알림 기능 시작

            // LolRankNotifier 초기화 (리그 오브 레전드)
            String lolUsername = "여자를뺏겼어요";
            String lolTag = "KR1";
            String lolChannelId = "325688449652883458"; // 리그 오브 레전드 알림을 보낼 채널 ID
            LolRankNotifier lolNotifier = new LolRankNotifier(jda, lolChannelId, lolUsername, lolTag);
            lolNotifier.start(60000); // 리그 오브 레전드 알림 기능 시작 (2분마다 체크)

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
