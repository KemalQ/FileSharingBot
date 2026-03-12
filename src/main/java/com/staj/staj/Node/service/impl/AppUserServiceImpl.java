package com.staj.staj.Node.service.impl;

import com.staj.staj.Node.service.AppUserService;
import com.staj.staj.commonUtils.dto.MailParams;
import com.staj.staj.commonUtils.utils.CryptoTool;
import com.staj.staj.common_jpa.dao.AppUserDAO;
import com.staj.staj.common_jpa.entity.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static com.staj.staj.common_jpa.entity.enums.UserState.BASIC_STATE;
import static com.staj.staj.common_jpa.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;

@Slf4j
@Service
public class AppUserServiceImpl implements AppUserService {
    private final AppUserDAO appUserDAO;
    private final CryptoTool cryptoTool;
    @Value("${service.mail.uri}")
    private String mailServiceUri;

    public AppUserServiceImpl(AppUserDAO appUserDAO, CryptoTool cryptoTool) {
        this.appUserDAO = appUserDAO;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public String registerUser(AppUser appUser) {
        if (appUser.getIsActive()){
            return "You are already registered!";
        } else if (appUser.getEmail() != null){
            return "An email has been sent to you. "
                    + "Follow the link in the email to confirm your registration.";
        }
        appUser.setState(WAIT_FOR_EMAIL_STATE);
        appUserDAO.save(appUser);
        return "Введите, пожалуйта ваш email:";
    }

    @Override
    public String setEmail(AppUser appUser, String email) {
        try {
            InternetAddress emailAddress = new InternetAddress(email);
            emailAddress.validate();
        } catch (AddressException e) {
            return "Please enter a valid email. To cancel the command, enter /cancel";
        }
        var optional = appUserDAO.findByEmail(email);//if not optional=null
        if (optional.isEmpty()) {
            appUser.setEmail(email);//TODO set email setEmail here is setter from AppUser
            appUser.setState(BASIC_STATE);
            appUser = appUserDAO.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());//TODO possibly an error due to returning a value from a hash
            var response = sendRequestToMailService(cryptoUserId, email);
            if (response.getStatusCode() != HttpStatus.OK) {
                var msg = String.format("Sending email to %s failed.", email);
                log.error(msg);
                appUser.setEmail(null);
                appUserDAO.save(appUser);
                return msg;
            }
            return "An email has been sent to you."
                    + " Follow the link in the email to confirm your registration.";
        }
        else {
            return "This email is already in use. Please enter a valid email."
                    + " To cancel a command, enter /cancel";
        }
    }
    private ResponseEntity<String> sendRequestToMailService(String cryptoUserId, String email) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var mailParams = MailParams.builder()
                .id(cryptoUserId)
                .emailTo(email)
                .build();
        var request = new HttpEntity<>(mailParams, headers);
        return restTemplate.exchange(mailServiceUri, HttpMethod.POST, request, String.class);//TODO check mailServiceUri
    }
}
