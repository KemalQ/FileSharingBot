package com.staj.staj.Node.service.impl;

import com.staj.staj.Node.exceptions.UploadFileException;
import com.staj.staj.Node.service.AppUserService;
import com.staj.staj.Node.service.FileService;
import com.staj.staj.Node.service.ProducerService;
import com.staj.staj.Node.dao.RawDataDAO;
import com.staj.staj.Node.entity.RawData;
import com.staj.staj.Node.service.MainService;
import com.staj.staj.Node.service.enums.LinkType;
import com.staj.staj.Node.service.enums.ServiceCommand;
import com.staj.staj.common_jpa.dao.AppUserDAO;
import com.staj.staj.common_jpa.entity.AppDocument;
import com.staj.staj.common_jpa.entity.AppPhoto;
import com.staj.staj.common_jpa.entity.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static com.staj.staj.Node.service.enums.ServiceCommand.*;
import static com.staj.staj.common_jpa.entity.enums.UserState.BASIC_STATE;
import static com.staj.staj.common_jpa.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;

@Service
@Slf4j
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;
    private final AppUserService appUserService;

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerService producerService, AppUserDAO appUserDAO, FileService fileService, AppUserService appUserService) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
        this.fileService = fileService;
        this.appUserService = appUserService;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();//достаем из телеграм. пользователя из входящего Update
        var output = "";

        var serviceCommand= ServiceCommand.fromValue(text);
        if (CANCEL.equals(serviceCommand)){
            output = cancelProcess(appUser);
        }
        else if(BASIC_STATE.equals(userState)){
            output = processServiceCommand(appUser, text);
        }
        else if(WAIT_FOR_EMAIL_STATE.equals(userState)){
            //TODO add email processing
            output = appUserService.setEmail(appUser, text);
        }
        else {
            log.error("Unknown user state: " + userState);
            output = "Unknown error! Enter /cancel and try again!";
        }
        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        try{
            AppDocument doc = fileService.processDoc(update.getMessage());
            String link = fileService.generateLink(doc.getId(), LinkType.GET_DOC);//Добавил генерацию ссылки для скачивания документа
            var answer = "Document successfully uploaded! Download link: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException ex) {
            log.error("Error loading file", ex);
            String error = "Sorry, file upload failed. Please try again later.";
            sendAnswer(error, chatId);
        }
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Register or activate " +
                    "your account to download content";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)){
            var error = "Cancel the current command with /cancel to send files";
            sendAnswer(error, chatId);
            return true;
        }
        return false;
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        try{//передал message из вход. Update в fileService
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            String link = fileService.generateLink(photo.getId(), LinkType.GET_PHOTO);//Добавил генерацию ссылки для скачивания фото
            var answer = "Photo uploaded successfully! " +
                    "Download link: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException ex) {
            log.error("Error uploading photo", ex);
            String error = "Sorry, photo upload failed. Please try again later.";
            sendAnswer(error, chatId);
        }
    }

    private String processServiceCommand(AppUser appUser, String cmd){
        var serviceCommand = ServiceCommand.fromValue(cmd);
        if (REGISTRATION.equals(serviceCommand)){
            //TODO add registration
            return appUserService.registerUser(appUser);
        } else if (HELP.equals(serviceCommand)){
            return help();
        } else if (START.equals(serviceCommand)) {
            return "Welcome! Enter /help to see list of available commands";
        } else return "Unknown command. Enter /help to see list of available commands";
    }

    private String help(){
        return "List of available commands:\n"
                + "/cancel - cancel execution of the current command\n"
                + "/registration - user registration";
    }

    private String cancelProcess(AppUser appUser){
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Command cancelled!";
    }

    private void sendAnswer(String output, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);//"Hello from NODE, this is text message"
        producerService.producerAnswer(sendMessage);
    }

    //private
    public AppUser findOrSaveAppUser(Update update){//veritabanında kullanıcıyı arıyor, PrimaryKey'e sahip ve Hibernate oturumuyla ilişkili
        User telegramUser = update.getMessage().getFrom();
        var optional = appUserDAO.findByTelegramUserId(telegramUser.getId());
        if(optional.isEmpty()){//если пользователя нет- сохраняем его
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .userName(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    //TODO change default value after adding registration
                    .isActive(false)
                    .state(BASIC_STATE).build();
            return appUserDAO.save(transientAppUser);
        }
        return optional.get();
    }
    private void saveRawData(Update update) {//storing to hash collection
        RawData rawData = RawData.builder().event(update).build();
        rawDataDAO.save(rawData);//storing to DB and setting id
    }
}
