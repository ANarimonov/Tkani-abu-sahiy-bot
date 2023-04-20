package uz.tkani_abusahiy_bot;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final AdminRepository adminRepository;
    private final PostRepository postRepository;
    @Value("${botToken}")
    private String botToken;
    @Value("${botUsername}")
    private String botUsername;
    @Override
    public void run(String... args) throws Exception {
        new TelegramBotsApi(DefaultBotSession.class).registerBot(new Bot(adminRepository, postRepository,botToken,botUsername));
    }
}
