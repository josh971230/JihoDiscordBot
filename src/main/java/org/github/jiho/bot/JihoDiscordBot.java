package org.github.jiho.bot;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.github.jiho.bot.response.ValCommand;
import org.github.jiho.bot.response.LolCommand;
import org.github.jiho.bot.response.TFTCommand;

import static org.github.jiho.bot.Constants.BOT_TOKEN;

    public class JihoDiscordBot {
        public static void main(String[] args) {
            JDABuilder.createDefault(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, new GatewayIntent[]{GatewayIntent.MESSAGE_CONTENT})
                    .setActivity(Activity.playing("박지호 감시"))
                    .addEventListeners(new Object[]{new ValCommand()})
                    .addEventListeners(new Object[]{new LolCommand()})
                    .addEventListeners(new Object[]{new TFTCommand()})
                    .build();
        }
    }
