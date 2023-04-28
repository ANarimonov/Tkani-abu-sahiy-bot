package uz.tkani_abusahiy_bot;

import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
        System.out.println(update);
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
                                case "Mahsulot qo'shish" -> {
                                    admin = adminRepository.save(admin.setStep(2));
                                    sendTextMessage(admin.getId(), "Material nomini kiriting");
                                }

                                case "Linklarni olish" -> {
                                    admin = adminRepository.save(admin.setStep(0));
                                    getAllLinks(admin.setStep(0));
                                }
                                case "Admin qo'shish" -> {
                                    admin = adminRepository.save(admin.setStep(4));
                                    SendMessage sm = new SendMessage(admin.getId().toString(), "Foydalanuvchi IDsini kiriting. IDni olish uchun u foydalanuvchi botga /getId buyrug'ini berishi kerak.");
                                    sm.setReplyMarkup(back());
                                    try {
                                        execute(sm);
                                    } catch (TelegramApiException e) {
                                        throw new RuntimeException(e);
                                    }

                                }
                                case "Adminni o'chirish" -> {
                                    admin = adminRepository.save(admin.setStep(5));
                                    SendMessage sm = new SendMessage(admin.getId().toString(), "Foydalanuvchi IDsini kiriting.");
                                    sm.setReplyMarkup(back());
                                    try {
                                        execute(sm);
                                    } catch (TelegramApiException e) {
                                        throw new RuntimeException(e);
                                    }

                                }


                            }
                        }
                    }
                    case 2 -> {
                        if (message.hasText()) {
                            Post post = new Post();
                            post.setTitle(text.toLowerCase());
                            postMap.put(userId, post);
                            sendTextMessage(admin.getId(), "Rasmlarni yuboring, yuborib bo'lgandan so'ng \"Linkni olish\" ni bosing");
                            adminRepository.save(admin.setStep(3));
                        }
                    }
                    case 3 -> {
                            try {
                                if (message.hasPhoto()) {
                                getPhotoFromTg(message, userId);
                                } else if (message.hasVideo()) {
                                    getVideo(message, userId);
                                } else if (message.hasAnimation()) {
                                    getAnimation(message, userId);
                                } else if (message.hasText() && text.equals("Linkni olish")) {
                                    admin = adminRepository.save(admin.setStep(1));
                                    sendTextMessage(admin.getId(), "https://t.me/" + botUsername + "?start=" + postMap.get(userId).getTitle());
                                postMap.remove(userId);
                                }
                            } catch (IOException |InterruptedException | TelegramApiException e) {
                                sendTextMessage(userId, "OOPS Rasm/Video jo'natilmadi qayta urinib ko'ring!");
                                throw new RuntimeException(e);
                            }

                    }
                    case 4 -> {
                        if (Objects.equals(text, "Ortga")) {
                            admin = adminRepository.save(admin.setStep(1));
                            sendTextMessage(admin.getId(), "Asosiy menu");
                        } else {
                            long id;
                            try {
                                id = Long.parseLong(text);
                            } catch (NumberFormatException e) {
                                throw new RuntimeException(e);
                            }
                            adminRepository.save(new Admin(id));
                            admin = adminRepository.save(admin.setStep(1));
                            sendTextMessage(admin.getId(), "Admin qo'shildi");
                        }
                    }
                    case 5 -> {
                        if (Objects.equals(text, "Ortga")) {
                            admin = adminRepository.save(admin.setStep(1));
                            sendTextMessage(admin.getId(), "Asosiy menu");
                        } else {
                            long id;
                            try {
                                id = Long.parseLong(text);
                            } catch (NumberFormatException e) {
                                throw new RuntimeException(e);
                            }
                            adminRepository.delete(adminRepository.findById(id).get());
                            admin = adminRepository.save(admin.setStep(1));
                            sendTextMessage(admin.getId(), "Admin o'chirildi");
                        }
                    }
                    default -> {
                        if (Objects.equals(text, "Ortga")) {
                            admin = adminRepository.save(admin.setStep(1));
                            sendTextMessage(admin.getId(), "Asosiy menu");
                        }
                    }
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
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            postRepository.deleteByTitle(callbackQuery.getData());
            Long id = callbackQuery.getFrom().getId();
            try {
                execute(new DeleteMessage(id.toString(), update.getCallbackQuery().getMessage().getMessageId()));
                execute(new SendMessage(id.toString(), "O'chirildi"));
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void getAllLinks(Admin admin) {
        List<String> allTitle = postRepository.findAllTitle();
        admin = adminRepository.save(admin.setStep(7));
        SendMessage sm = new SendMessage(admin.getId().toString(), "Linklar:");
        sm.setReplyMarkup(back());
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        if (allTitle.size() == 0) {
            sendTextMessage(admin.getId(), "Ayni vaqtda linklar yo'q");
        } else {

            for (String title : allTitle) {
                sendTextMessage(admin.getId(), "https://t.me/" + botUsername + "?start=" + title, title);
            }
        }
    }

    private void getVideo(Message message, Long userId) throws IOException, TelegramApiException {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(channelId);
        sendVideo.setVideo(new InputFile(message.getVideo().getFileId()));
        Message execute = execute(sendVideo);
        Post post = postMap.get(userId);
        post.setMessageId(execute.getMessageId());
        postRepository.save(post);
        sendTextMessage(userId, "Yana rasm/video bo'lsa yuboring yoki \"Linkni olish\" ni bosing");

    }
private void getAnimation(Message message, Long userId) throws IOException, TelegramApiException {
        SendAnimation sendAnimation = new SendAnimation();
    sendAnimation.setChatId(channelId);
    sendAnimation.setAnimation(new InputFile(message.getAnimation().getFileId()));
        Message execute = execute(sendAnimation);
        Post post = postMap.get(userId);
        post.setMessageId(execute.getMessageId());
        postRepository.save(post);
        sendTextMessage(userId, "Yana rasm/video bo'lsa yuboring yoki \"Linkni olish\" ni bosing");

    }

    private void getPhotoFromTg(Message message, Long userId) throws IOException, TelegramApiException, InterruptedException {
        SendPhoto sendVideo = new SendPhoto();
        sendVideo.setChatId(channelId);
        sendVideo.setPhoto(new InputFile(message.getPhoto().get(message.getPhoto().size()-1).getFileId()));
        Message execute = execute(sendVideo);
        Post post = postMap.get(userId);
        post.setMessageId(execute.getMessageId());
        postRepository.save(post);
        sendTextMessage(userId, "Yana rasm/video bo'lsa yuboring yoki \"Linkni olish\" ni bosing");

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

    private ReplyKeyboard back() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rowList = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton row1Button1 = new KeyboardButton();
        row1Button1.setText("Ortga");
        row1.add(row1Button1);
        rowList.add(row1);
        markup.setKeyboard(rowList);
        markup.setSelective(true);
        markup.setResizeKeyboard(true);
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

}
