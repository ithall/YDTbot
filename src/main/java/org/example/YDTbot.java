package org.example;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.OkHttpDownloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
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
        // Инициализация NewPipe
        NewPipe.init(OkHttpDownloader.builder().build());
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
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String text = update.getMessage().getText().trim();
        String chatId = update.getMessage().getChatId().toString();

        if (text.startsWith("/start")) {
            sendMessage(chatId, "Пришли ссылку на YouTube видео 🎥\nЯ попробую скачать видео или аудио.");
            return;
        }

        if (!text.contains("youtube.com") && !text.contains("youtu.be")) {
            sendMessage(chatId, "Это не похоже на YouTube-ссылку. Пришли правильную ссылку.");
            return;
        }

        sendMessage(chatId, "Обрабатываю… подожди 10–30 секунд ⏳");

        try {
            String streamUrl = getBestStreamUrl(text);

            if (streamUrl == null || streamUrl.isEmpty()) {
                sendMessage(chatId, "Не удалось найти поток 😔 Попробуй другую ссылку.");
                return;
            }

            File tempFile = downloadFile(streamUrl, "video.mp4");

            if (tempFile.length() < 50 * 1024 * 1024) {  // меньше 50 МБ
                SendDocument doc = new SendDocument();
                doc.setChatId(chatId);
                doc.setDocument(new InputFile(tempFile));
                doc.setCaption("Вот видео (лучшее качество с аудио)");
                execute(doc);
            } else {
                sendMessage(chatId, "Файл слишком большой (>50 МБ) для отправки.\nПрямая ссылка на скачивание:\n" + streamUrl);
            }

            tempFile.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "Ошибка: " + e.getMessage().substring(0, Math.min(150, e.getMessage().length())) + "...");
        }
    }

    private String getBestStreamUrl(String youtubeUrl) throws Exception {
        LinkHandler handler = ServiceList.YouTube.getStreamLHFactory().fromUrl(youtubeUrl);
        StreamInfo info = StreamInfo.getInfo(handler.getUrl());

        // Пытаемся взять видео + аудио поток
        List<VideoStream> videoStreams = info.getVideoStreams();
        for (VideoStream vs : videoStreams) {
            if (!vs.isVideoOnly()) {  // содержит аудио
                return vs.getContent();
            }
        }

        // Если нет — любой видео-поток
        if (!videoStreams.isEmpty()) {
            return videoStreams.get(0).getContent();
        }

        // Или аудио
        if (!info.getAudioStreams().isEmpty()) {
            return info.getAudioStreams().get(0).getContent();
        }

        return null;
    }

    private File downloadFile(String urlStr, String fileName) throws IOException {
        File file = File.createTempFile("yt_", ".mp4");
        try (InputStream in = new URL(urlStr).openStream();
             FileOutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
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
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new YoutubeDownloaderBot());
            System.out.println("Бот запущен и готов к работе!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
              }
