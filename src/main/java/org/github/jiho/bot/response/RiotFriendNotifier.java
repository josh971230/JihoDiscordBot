package org.github.jiho.bot.response;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

public class RiotFriendNotifier {

    private final JDA jda;
    private final String channelId;
    private static final String LOCKFILE_PATH = System.getenv("LOCALAPPDATA") + "\\Riot Games\\Riot Client\\Config\\lockfile";
    private static final String FRIEND_NAME = "단비우#0411"; // 확인할 친구 이름

    public RiotFriendNotifier(JDA jda, String channelId) {
        this.jda = jda;
        this.channelId = channelId;
    }

    public void start() {
        System.out.println("Starting RiotFriendNotifier...");
        Timer timer = new Timer(true);

        try {
            LockfileInfo lockfileInfo = readLockfile();
            System.out.println("Lockfile read successfully. Port: " + lockfileInfo.port + ", Password: " + lockfileInfo.password);

            // 주기적으로 친구 상태 확인 (1분마다 실행)
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        checkFriendStatus(lockfileInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 60000); // 1분마다 확인
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LockfileInfo readLockfile() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(LOCKFILE_PATH))) {
            String[] lockfileData = br.readLine().split(":");
            return new LockfileInfo(lockfileData[2], lockfileData[3]);
        }
    }

    private void checkFriendStatus(LockfileInfo lockfileInfo) throws Exception {
        HttpClient client = createHttpClientWithDisabledSSL();
        String auth = Base64.getEncoder().encodeToString(("riot:" + lockfileInfo.password).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://127.0.0.1:" + lockfileInfo.port + "/chat/v4/friends"))
                .header("Authorization", "Basic " + auth)
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String responseBody = response.body();
            if (responseBody.contains("\"game_name\":\"단비우\"") && responseBody.contains("\"game_tag\":\"0411\"") && responseBody.contains("\"availability\":\"online\"")) {
                sendOnlineNotification();
            } else {
                System.out.println(FRIEND_NAME + " is not online.");
            }
        } else {
            System.err.println("Failed to get friend list, response code: " + response.statusCode());
        }
    }

    private void sendOnlineNotification() {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(FRIEND_NAME + " is online!").queue();
        } else {
            System.err.println("Channel with ID " + channelId + " not found.");
        }
    }

    private HttpClient createHttpClientWithDisabledSSL() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();
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
