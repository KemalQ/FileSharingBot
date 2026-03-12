package com.staj.staj.Node.service.impl;

import com.staj.staj.Node.exceptions.UploadFileException;
import com.staj.staj.Node.service.FileService;
import com.staj.staj.Node.service.enums.LinkType;
import com.staj.staj.commonUtils.utils.CryptoTool;
import com.staj.staj.common_jpa.dao.AppDocumentDAO;
import com.staj.staj.common_jpa.dao.AppPhotoDAO;
import com.staj.staj.common_jpa.dao.BinaryContentDAO;
import com.staj.staj.common_jpa.entity.AppDocument;
import com.staj.staj.common_jpa.entity.AppPhoto;
import com.staj.staj.common_jpa.entity.BinaryContent;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    @Value("${telegram.bot.token}")
    private String token;
    @Value("${service.file_info.uri}")
    private String fileInfoUri;
    @Value("${service.file_storage.uri}")
    private String fileStorageUri;
    @Value("${link.address}")
    public String linkAddress;
    private final AppDocumentDAO appDocumentDAO;
    private final BinaryContentDAO binaryContentDAO;
    private final AppPhotoDAO appPhotoDAO;//bean для сохранения объекта фото в базу
    private final CryptoTool cryptoTool;

    public FileServiceImpl(AppDocumentDAO appDocumentDAO, BinaryContentDAO binaryContentDAO, AppPhotoDAO appPhotoDAO, CryptoTool cryptoTool) {
        this.appDocumentDAO = appDocumentDAO;
        this.binaryContentDAO = binaryContentDAO;
        this.appPhotoDAO = appPhotoDAO;
        this.cryptoTool = cryptoTool;
    }
    @Override
    public AppDocument processDoc(Message telegramMessage) {
        if(telegramMessage.getDocument() == null) {
            return null;
        }
        Document telegramDoc = telegramMessage.getDocument();
        String fileId = telegramDoc.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK){
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppDocument transientAppDoc = buildTransientAppDoc(telegramDoc, persistentBinaryContent);
            return appDocumentDAO.save(transientAppDoc);
        } else {
            throw new UploadFileException("Bad response from telegram service: " + response);
        }
    }
    @Override
    public AppPhoto processPhoto(Message telegramMessage) {
        //TODO telega позволяет скачать фото в нескольких разрегшениях, поэтому указал последний объект
        var photoSizeCount = telegramMessage.getPhoto().size();
        var photoIndex = photoSizeCount >1 ? telegramMessage.getPhoto().size() -1 : 0;
        PhotoSize telegramPhoto = telegramMessage.getPhoto().get(photoIndex);//TODO тут
        String fileId = telegramPhoto.getFileId();// Получаем уникальный идентификатор файла в Telegram
        ResponseEntity<String> response = getFilePath(fileId);// Делаем запрос к API Telegram, чтобы получить путь к файлу
        if (response.getStatusCode() == HttpStatus.OK){// Проверяем успешность ответа
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);// Получаем и сохраняем бинарное содержимое файла
            // Создаем объект AppPhoto с данными из Telegram и сохраненным содержимым
            AppPhoto transientAppDoc = buildTransientAppPhoto(telegramPhoto, persistentBinaryContent);
            return appPhotoDAO.save(transientAppDoc);// Сохраняем фото в базу данных и возвращаем сохраненный объект
        } else {
            throw new UploadFileException("Bad response from telegram service: " + response);
        }
    }
    private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) {
        String filePath = getFilePath(response);//получение пути из
        byte[] fileInByte = downloadFile(filePath);//запрос к серверу телеги, скачка в виде байт
        BinaryContent transientBinaryContent = BinaryContent.//через builder-build создается объект класса BinaryContent
                builder().fileAsArrayOfBytes(fileInByte).build();
        return binaryContentDAO.save(transientBinaryContent);//сохраняем-возвращаем этот объект в таблице "binary_content"
    }

    private String getFilePath(ResponseEntity<String> response) {
        JSONObject jsonObject = new JSONObject(response.getBody());//Создаем JSON объект из тела ответа


        return String.valueOf(jsonObject. //Извлекаем значение file_path из вложенного объекта result
                getJSONObject("result").// и преобразуем его в строку
                getString("file_path"));
    }
    private AppDocument buildTransientAppDoc(Document telegramDoc, BinaryContent persistentBinaryContent) {
        return AppDocument.builder()//достает значения из полей телеграмовского объекта документа и сетит их в наш объект
                .telegramFieldId(telegramDoc.getFileId())
                .docName(telegramDoc.getFileName())
                .binaryContent(persistentBinaryContent)
                .mimeType(telegramDoc.getMimeType())
                .fileSize(telegramDoc.getFileSize())
                .build();
    }
    private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto, BinaryContent persistentBinaryContent) {
        return AppPhoto.builder()//достает значения из полей телеграмовского объекта документа и сетить их в наш объект
                .telegramFiledId(telegramPhoto.getFileId())//установка id файла, полученный из Telegram
                .binaryContent(persistentBinaryContent)// Установка бинарное содержимое файла
                .fileSize(telegramPhoto.getFileSize())// Установка размера файла из данных Telegram
                .build();
    }
    private ResponseEntity<String> getFilePath(String fileId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);

        return restTemplate.exchange(fileInfoUri, HttpMethod.GET,
               request,String.class, token, fileId);
    }
    private byte[] downloadFile(String filePath) {
        var fullUri = fileStorageUri.replace("{token}", token)
                .replace("{filePath}", filePath);
        URL urlObj = null;//сначала null, потом -переопределил
        try {
            urlObj = new URL(fullUri);
        } catch (MalformedURLException e) {
            throw new UploadFileException(e);
        }

        //TODO подумать над оптимизацией
        try (InputStream is = urlObj.openStream()) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UploadFileException(urlObj.toExternalForm(), e);
        }
    }
    @Override
    public String generateLink(Long docId, LinkType linkType) {
        var hash = cryptoTool.hashOf(docId);
        return "http://" + linkAddress + "/" + linkType + "?id=" + hash;
    }
}
