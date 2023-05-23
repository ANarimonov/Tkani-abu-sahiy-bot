package uz.tkani_abusahiy_bot;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final AdminRepository adminRepository;
    private final PostRepository postRepository;
    @Value("${botToken}")
    private String botToken;
    @Value("${botUsername}")
    private String botUsername;
    @Value("${spring.datasource.url}")
    private String dBUrl;
    @Value("${spring.datasource.username}")
    private String dBUser;
    @Value("${spring.datasource.password}")
    private String dBPassword;

    @Override
    public void run(String... args) throws Exception {
        Bot bot = new Bot(adminRepository, postRepository, botToken, botUsername);
        new TelegramBotsApi(DefaultBotSession.class).registerBot(bot);
        while (true) {
            Thread thread = new Thread(() -> {
                String s = LocalDate.now().toString().substring(8);
                if (s.equals("23")) {
                    databaseBackup(bot);
                }
            });
            thread.start();
            try {
                Thread.sleep(86_400_000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void databaseBackup(Bot bot) {
        long programmerTGId = 905951214L;
        String[] split = dBUrl.split("/");
        String dbName = Arrays.stream(split).toList().get(split.length - 1);
        String backupPath = "backup.sql";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/usr/lib/postgresql/14/bin/pg_dump", "-U", dBUser, "-d", dbName);
            processBuilder.environment().put("PGPASSWORD", dBPassword);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                FileOutputStream fos = new FileOutputStream(backupPath);
                fos.write(stringBuilder.toString().getBytes());
                fos.flush();
                fos.close();
                bot.sendDocument(programmerTGId, backupPath);
            } else {
                System.out.println("Backup failed. Exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
