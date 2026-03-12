package com.staj.staj.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;


@Component
@Slf4j
public class TelegramBot extends TelegramWebhookBot {
    @Value("${telegram.bot.username}")
    private String userName;
    @Value("${telegram.bot.token}")
    private String tokenBot;
    @Value("${bot.uri}")
    private String botUri;
    private final UpdateProcessor updateProcessor;

    public TelegramBot(UpdateProcessor updateProcessor) {
        this.updateProcessor = updateProcessor;
    }

    @PostConstruct
    public void init() {
        updateProcessor.registerBot(this);
        try {
            var setWebhook = SetWebhook.builder().url(botUri).build();
            this.setWebhook(setWebhook);
        } catch (TelegramApiException e) {
            log.error(String.valueOf(e));
        }
    }

    @Override
    public String getBotUsername() {
        return userName;
    }

    @Override
    public String getBotToken() {
        return tokenBot;
    }

    @Override
    public String getBotPath() {
        return "/update";
    }

    public void sendAnswerMessage(SendMessage message) {
        if (message != null) {
            try{
                execute(message);
            } catch (TelegramApiException e){
                log.error(String.valueOf(e));
            }
        }
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return null;
    }
}
