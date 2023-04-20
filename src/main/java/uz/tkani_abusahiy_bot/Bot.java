package uz.tkani_abusahiy_bot;

import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {
    private final AdminRepository adminRepository;
    private final PostRepository postRepository;
    private final Long channelId = -1001562049037L;
    private final Map<Long, Post> postMap = new HashMap<>();
    private final String botToken;
    private final String botUsername;


    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long userId = message.getFrom().getId();
            if (message.hasText() && message.getText().equalsIgnoreCase("/getId"))
                sendTextMessage(userId, message.getFrom().getId().toString());
            if (adminRepository.existsAdminById(userId)) {
                Admin admin = adminRepository.findById(userId).get();
                String text = message.getText();
                if (message.hasText() && text.equals("/start")) {
                    admin = adminRepository.save(admin.setStep(0));
                }
                switch (admin.getStep()) {
                    case 0 -> {
                        admin = adminRepository.save(admin.setStep(1));
                        sendTextMessage(admin.getId(), "Asosiy menu");
                    }
                    case 1 -> {
                        if (message.hasText()) {
                            switch (text) {
                                case "Mahsulot qo'shish" ->
                                        sendTextMessage(admin.setStep(2).getId(), "Material nomini kiriting");
                                case "Linklarni olish" -> getAllLinks(admin.setStep(0));
                                case "Admin qo'shish" ->
                                        sendTextMessage(admin.setStep(4).getId(), "Foydalanuvchi IDsini kiriting. IDni olish uchun u foydalanuvchi botga /getId buyrug'ini berishi kerak.");
                                case "Adminni o'chirish" ->
                                        sendTextMessage(admin.setStep(5).getId(), "Foydalanuvchi IDsini kiriting.");

                            }
                        }
                    }
                    case 2 -> {
                        if (message.hasText()) {
                            Post post = new Post();
                            post.setTitle(text.toLowerCase());
                            postMap.put(userId, post);
                            sendTextMessage(admin.setStep(3).getId(), "Rasmlarni yuboring, yuborib bo'lgandan so'ng \"Linkni olish\" ni bosing");
                        }
                    }
                    case 3 -> {
                        if (message.hasPhoto()) {
                            getPhotoFromTg(message, userId);
                        } else if (message.hasVideo()) {

                        } else if (message.hasText() && text.equals("Linkni olish")) {

                            sendTextMessage(admin.setStep(1).getId(), "https://t.me/" + botUsername + "?start=" + postMap.get(userId).getTitle());
                        }
                    }
                    case 4 -> adminRepository.save(new Admin(Long.parseLong(text)));
                    case 5 -> adminRepository.delete(adminRepository.findById(Long.parseLong(text)).get());
                }
                adminRepository.save(admin);
            } else {
                if (message.hasText()) {
                    String[] strings = message.getText().split(" ");
                    if (strings[0].equals("/start")) {
                        List<Post> posts = postRepository.findByTitle(strings[1]);
                        for (Post post : posts) {
                            sendForwardMessage(userId, channelId, post.getMessageId());
                        }
                    }
                }
            }
        }
        else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            postRepository.deleteByTitle(callbackQuery.getData());
            Long id = callbackQuery.getFrom().getId();
            adminRepository.save(new Admin(id, 1));
            sendTextMessage(id, "O'chirildi");
        }
    }

    private void getAllLinks(Admin admin) {
        List<String> allTitle = postRepository.findAllTitle();
        for (String title : allTitle) {
            admin = adminRepository.save(admin.setStep(7));
            sendTextMessage(admin.getId(), "https://t.me/" + botUsername + "?start=" + title, title);
        }
    }

    private void getPhotoFromTg(Message message, Long userId) {
        File file;
        GetFile getFile = new GetFile();
        List<PhotoSize> photoSizes = message.getPhoto();
        PhotoSize photoSize = photoSizes.get(photoSizes.size() - 1);
        getFile.setFileId(photoSize.getFileId());

        try {
            file = execute(getFile);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        OkHttpClient client = new OkHttpClient();
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("api.telegram.org")
                .addPathSegment("file")
                .addPathSegment("bot" + botToken)
                .addPathSegment(file.getFilePath())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response downloadResponse = client.newCall(request).execute();
            InputStream inputStream = downloadResponse.body().byteStream();
            Message message1 = sendPhotoMessage(inputStream);
            Post post = postMap.get(userId);
            post.setMessageId(message1.getMessageId());
            postRepository.save(post);
            sendTextMessage(userId, "Yana rasm/video bo'lsa yuboring yoki \"Linkni olish\" ni bosing");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ReplyKeyboard getReplyMarkup(Admin admin, String title) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        markup.setKeyboard(rows);
        switch (admin.getStep()) {
            case 1 -> {
                KeyboardRow row = new KeyboardRow();
                row.add("Mahsulot qo'shish");
                row.add("Linklarni olish");
                rows.add(row);
                row = new KeyboardRow();
                row.add("Admin qo'shish");
                row.add("Adminni o'chirish");
                rows.add(row);
            }
            case 3 -> {
                KeyboardRow row = new KeyboardRow();
                row.add("Linkni olish");
                rows.add(row);
            }
            case 6 -> {
                List<String> allTitle = postRepository.findAllTitle();
                KeyboardRow row = new KeyboardRow();
                int c = 0;
                for (String s : allTitle) {
                    row.add(s);
                    c++;
                    if (c % 2 == 1) {
                        rows.add(row);
                        row = new KeyboardRow();
                    }
                }
                rows.add(row);
            }
            case 7 -> {
                InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("O'chirish");
                button.setCallbackData(title);
                row.add(button);
                inlineRows.add(row);
                inlineMarkup.setKeyboard(inlineRows);
                return inlineMarkup;
            }
        }
        return markup;
    }

    private void sendForwardMessage(Long toChatId, Long fromChatId, int messageId) {
        ForwardMessage forwardMessage = new ForwardMessage();
        forwardMessage.setChatId(toChatId);
        forwardMessage.setMessageId(messageId);
        forwardMessage.setFromChatId(fromChatId);
        try {
            execute(forwardMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendTextMessage(Long userId, String text) {
        SendMessage sendMessage = new SendMessage();
        adminRepository.findById(userId).ifPresent(admin -> sendMessage.setReplyMarkup(getReplyMarkup(admin, null)));
        sendMessage.setChatId(userId);
        sendMessage.setText(text);
        sendMessage.setParseMode("html");
        sendMessage.setDisableWebPagePreview(true);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendTextMessage(Long userId, String text, String title) {
        SendMessage sendMessage = new SendMessage();
        adminRepository.findById(userId).ifPresent(admin -> sendMessage.setReplyMarkup(getReplyMarkup(admin, title)));
        sendMessage.setChatId(userId);
        sendMessage.setText(text);
        sendMessage.setParseMode("html");
        sendMessage.setDisableWebPagePreview(true);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    private Message sendPhotoMessage(InputStream inputStream) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(channelId);
        sendPhoto.setPhoto(new InputFile(inputStream, UUID.randomUUID().toString()));
        try {
            return execute(sendPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
