package org.example;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class YDTbot extends TelegramLongPollingBot {

    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    private static final String BOT_USERNAME = System.getenv("BOT_USERNAME");

    public YDTbot() {
        NewPipe.init();
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        String chatId = update.getMessage().getChatId().toString();

        if (text.startsWith("/start")) {
            sendMessage(chatId, "Пришли ссылку на YouTube видео 🎥\nЯ попробую скачать.");
            return;
        }

        if (!text.contains("youtube.com") && !text.contains("youtu.be")) {
            sendMessage(chatId, "Это не YouTube-ссылка.");
            return;
        }

        sendMessage(chatId, "Обрабатываю... подожди ⏳");

        try {
            String url = getBestUrl(text);
            if (url == null || url.isEmpty()) {
                sendMessage(chatId, "Не удалось найти поток.");
                return;
            }

            File file = downloadToFile(url);

            if (file.length() < 50 * 1024 * 1024) {
                SendDocument doc = new SendDocument();
                doc.setChatId(chatId);
                doc.setDocument(new InputFile(file));
                doc.setCaption("Вот видео (с аудио)");
                execute(doc);
            } else {
                sendMessage(chatId, "Файл большой (>50 МБ).\nПрямая ссылка: " + url);
            }

            file.deleteOnExit();

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }

    private String getBestUrl(String youtubeUrl) throws Exception {
        StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube, youtubeUrl);

        List<VideoStream> videos = info.getVideoStreams();
        for (VideoStream v : videos) {
            if (!v.isVideoOnly()) return v.getContent();
        }
        if (!videos.isEmpty()) return videos.get(0).getContent();

        if (!info.getAudioStreams().isEmpty()) return info.getAudioStreams().get(0).getContent();

        return null;
    }

    private File downloadToFile(String urlStr) throws IOException {
        File file = File.createTempFile("ydt_", ".mp4");
        try (InputStream in = new URL(urlStr).openStream();
             FileOutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytes;
            while ((bytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
            }
        }
        return file;
    }

    private void sendMessage(String chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        try {
            execute(msg);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new YDTbot());
        System.out.println("YDTbot запущен!");
    }
                  }
