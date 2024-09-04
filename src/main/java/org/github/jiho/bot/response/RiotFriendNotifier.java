package org.github.jiho.bot.response;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class RiotFriendNotifier {

    private static final String LOCKFILE_PATH = System.getenv("LOCALAPPDATA") + "\\Riot Games\\Riot Client\\Config\\lockfile";
    private static final String FRIEND_NAME = "단비우#0411"; // 확인할 친구 이름

    private final JDA jda;  // JDA 인스턴스
    private final String channelId;  // 알림을 보낼 채널 ID
    private boolean wasOnline = false; // 이전에 온라인이었는지 여부를 저장하는 변수

    public RiotFriendNotifier(JDA jda, String channelId) {
        this.jda = jda;
        this.channelId = channelId;
    }

    public void start() {
        try {
            System.out.println("Starting RiotFriendNotifier...");

            LockfileInfo lockfileInfo = readLockfile();
            System.out.println("Lockfile read successfully. Port: " + lockfileInfo.port + ", Password: " + lockfileInfo.password);

            while (true) {
                checkFriendStatus(lockfileInfo);
                Thread.sleep(60000); // 1분마다 확인
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LockfileInfo readLockfile() throws IOException {
        System.out.println("Reading lockfile...");

        try (BufferedReader br = new BufferedReader(new FileReader(LOCKFILE_PATH))) {
            String[] lockfileData = br.readLine().split(":");
            System.out.println("Lockfile data: " + String.join(",", lockfileData));
            return new LockfileInfo(lockfileData[2], lockfileData[3]);
        } catch (IOException e) {
            System.err.println("Failed to read lockfile: " + e.getMessage());
            throw e;
        }
    }

    private void checkFriendStatus(LockfileInfo lockfileInfo) throws Exception {
        System.out.println("Checking friend status...");

        HttpClient client = HttpClient.newHttpClient();
        String auth = Base64.getEncoder().encodeToString(("riot:" + lockfileInfo.password).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://127.0.0.1:" + lockfileInfo.port + "/chat/v4/friends"))
                .header("Authorization", "Basic " + auth)
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Received response with status code: " + response.statusCode());

        if (response.statusCode() == 200) {
            String responseBody = response.body();
            System.out.println("Response body: " + responseBody);

            boolean isOnline = responseBody.contains("\"game_name\":\"단비우\"") &&
                    responseBody.contains("\"game_tag\":\"0411\"") &&
                    responseBody.contains("\"availability\":\"online\"");

            // 상태 변화 감지: 오프라인 -> 온라인
            if (isOnline && !wasOnline) {
                sendOnlineNotification();  // 온라인 알림을 보냄
                wasOnline = true;  // 온라인 상태로 업데이트
            }
            // 상태 변화 감지: 온라인 -> 오프라인
            else if (!isOnline && wasOnline) {
                wasOnline = false;  // 오프라인 상태로 업데이트
            }

        } else {
            System.err.println("Failed to get friend list, response code: " + response.statusCode());
        }
    }

    private void sendOnlineNotification() {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Friend Status Update");
            eb.setDescription(FRIEND_NAME + " is now online!");

            eb.setColor(0x00FF00); // 녹색
            channel.sendMessageEmbeds(eb.build()).queue();
            System.out.println(FRIEND_NAME + " is online! Notification sent.");
        } else {
            System.err.println("Channel with ID " + channelId + " not found.");
        }
    }

    private static class LockfileInfo {
        String port;
        String password;

        LockfileInfo(String port, String password) {
            this.port = port;
            this.password = password;
        }
    }
}


