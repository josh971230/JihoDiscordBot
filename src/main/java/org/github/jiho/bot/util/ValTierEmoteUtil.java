package org.github.jiho.bot.util;

public class ValTierEmoteUtil {

    /**
     * 주어진 티어 정보를 바탕으로 해당하는 발로란트 이모티콘을 반환합니다.
     *
     * @param tierInfo 티어 정보 문자열 (예: "Platinum 2")
     * @return 해당 티어에 대응하는 이모티콘 문자열
     */
    public static String getTierEmote(String tierInfo) {
        if (tierInfo.contains("Iron 1")) {
            return "<:Iron_1_Rank:1280414627909337088>";
        } else if (tierInfo.contains("Iron 2")) {
            return "<:Iron_2_Rank:1280414630035853403>";
        } else if (tierInfo.contains("Iron 3")) {
            return "<:Iron_3_Rank:1280414631709638709>";
        } else if (tierInfo.contains("Bronze 1")) {
            return "<:Bronze_1_Rank:1280414710415491154>";
        } else if (tierInfo.contains("Bronze 2")) {
            return "<:Bronze_2_Rank:1280414730342764564>";
        } else if (tierInfo.contains("Bronze 3")) {
            return "<:Bronze_3_Rank:1280414753877135370>";
        } else if (tierInfo.contains("Silver 1")) {
            return "<:Silver_1_Rank:1280414815357239388>";
        } else if (tierInfo.contains("Silver 2")) {
            return "<:Silver_2_Rank:1280414812282687488>";
        } else if (tierInfo.contains("Silver 3")) {
            return "<:Silver_3_Rank:1280414813864071199>";
        } else if (tierInfo.contains("Gold 1")) {
            return "<:Gold_1_Rank:1280414852522708992>";
        } else if (tierInfo.contains("Gold 2")) {
            return "<:Gold_2_Rank:1280414854464671787>";
        } else if (tierInfo.contains("Gold 3")) {
            return "<:Gold_3_Rank:1280414856511619082>";
        } else if (tierInfo.contains("Platinum 1")) {
            return "<:Platinum_1_Rank:1280414919396823050>";
        } else if (tierInfo.contains("Platinum 2")) {
            return "<:Platinum_2_Rank:1280414920537673770>";
        } else if (tierInfo.contains("Platinum 3")) {
            return "<:Platinum_3_Rank:1280414922617913375>";
        } else if (tierInfo.contains("Diamond 1")) {
            return "<:Diamond_1_Rank:1280414980818337910>";
        } else if (tierInfo.contains("Diamond 2")) {
            return "<:Diamond_2_Rank:1280414982865162309>";
        } else if (tierInfo.contains("Diamond 3")) {
            return "<:Diamond_3_Rank:1280414979568177272>";
        } else if (tierInfo.contains("Ascendant 1")) {
            return "<:Ascendant_1_Rank:1280415033867898943>";
        } else if (tierInfo.contains("Ascendant 2")) {
            return "<:Ascendant_2_Rank:1280415035511930880>";
        } else if (tierInfo.contains("Ascendant 3")) {
            return "<:Ascendant_3_Rank:1280415037751689269>";
        } else if (tierInfo.contains("Immortal 1")) {
            return "<:Immortal_1_Rank:1280415084488949760>";
        } else if (tierInfo.contains("Immortal 2")) {
            return "<:Immortal_2_Rank:1280415082563768372>";
        } else if (tierInfo.contains("Immortal 3")) {
            return "<:Immortal_3_Rank:1280415086451621898>";
        } else if (tierInfo.contains("Radiant")) {
            return "<:Radiant_Rank:1280415123013632131>";
        } else {
            return "<:unknown:1234567890123456>"; // Unknown tier emoji
        }
    }
}
