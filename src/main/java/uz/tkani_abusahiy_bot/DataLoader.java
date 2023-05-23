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
    }
}
