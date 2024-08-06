package ru.panic.tgdispatchbot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.panic.tgdispatchbot.property.TelegramBotProperty;

@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramBotProperty telegramBotProperty;

    @Override
    public String getBotUsername() {
        return "empty";
    }

    @Override
    public String getBotToken() {
        return telegramBotProperty.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update.getMessage().getText());
    }
}
