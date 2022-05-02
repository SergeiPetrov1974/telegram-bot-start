package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationRepository;

import javax.annotation.PostConstruct;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final NotificationRepository notificationRepository;
    private final TelegramBot telegramBot;

    public TelegramBotUpdatesListener(pro.sky.telegrambot.repository.NotificationRepository notificationRepository, TelegramBot telegramBot) {
        this.notificationRepository = notificationRepository;
        this.telegramBot = telegramBot;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            // Process your updates here
            long chatId = update.message().chat().id();
            if (update.message().text().equals("/start")) {
                String messageText = "Отправь уведомление в формате: 28.04.2022 22:17 Сделать домашнюю работу";
                SendMessage message = new SendMessage(chatId, messageText);
                SendResponse response = telegramBot.execute(message);
            } else saveNotification(update.message().text(), chatId);
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public String parsingMessage(int number, String text) {
        Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.matches()) {
            text = matcher.group(number);
        }

        return text;
    }

    public void saveNotification(String text, Long chatId) {
        try {
            LocalDateTime date = LocalDateTime.parse(parsingMessage(1, text), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String notification = parsingMessage(3, text);
            logger.info("Дата:{}", date);
            logger.info("Уведомление:{}", notification);
            NotificationTask notificationTask = new NotificationTask();
            notificationTask.setIdChat(chatId);
            notificationTask.setNotification(notification);
            notificationTask.setDate(date);
            notificationRepository.save(notificationTask);

        } catch (Exception e) {
            logger.info("Введите дату в правильном формате");
        }


    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void searchNotification() {
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<NotificationTask> currentTasks = notificationRepository.getByDate(currentTime);
        if (currentTasks.size() > 0) {
            logger.info("уведомление отправлено");
            currentTasks.forEach(current_task -> {
                SendMessage message = new SendMessage(current_task.getIdChat(), current_task.getNotification());
                SendResponse response = telegramBot.execute(message);
            });

        } else logger.info(("нет новых сообщений"));

    }
}

