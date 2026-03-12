package com.staj.staj.Node.service;

import com.staj.staj.Node.service.impl.MainServiceImpl;
import com.staj.staj.common_jpa.dao.AppUserDAO;
import com.staj.staj.common_jpa.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

import static com.staj.staj.common_jpa.entity.enums.UserState.BASIC_STATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainServiceImplUnitTest {

    @Mock
    private AppUserDAO appUserDAO;

    @InjectMocks
    private MainServiceImpl mainService;

    private Update update;
    private User telegramUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        telegramUser = new User();
        telegramUser.setId(12345L);
        telegramUser.setUserName("testuser");
        telegramUser.setFirstName("Test");
        telegramUser.setLastName("User");

        Message message = new Message();
        message.setFrom(telegramUser);

        update = new Update();
        update.setMessage(message);
    }

    @Test
    void findOrSaveAppUser_shouldReturnExistingUser_whenUserExists() {
        AppUser existingUser = AppUser.builder()
                .telegramUserId(12345L)
                .userName("testuser")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .state(BASIC_STATE)
                .build();

        when(appUserDAO.findByTelegramUserId(12345L))
                .thenReturn(Optional.of(existingUser));

        AppUser result = mainService.findOrSaveAppUser(update);

        assertSame(existingUser, result);
        verify(appUserDAO, never()).save(any());
    }

    @Test
    void findOrSaveAppUser_shouldSaveNewUser_whenUserNotExists() {
        when(appUserDAO.findByTelegramUserId(12345L))
                .thenReturn(Optional.empty());
        AppUser savedUser = AppUser.builder()
                .telegramUserId(12345L)
                .userName("testuser")
                .firstName("Test")
                .lastName("User")
                .isActive(false)
                .state(BASIC_STATE)
                .build();
        when(appUserDAO.save(any(AppUser.class))).thenReturn(savedUser);
        AppUser result = mainService.findOrSaveAppUser(update);
        assertNotNull(result);
        assertEquals(12345L, result.getTelegramUserId());
        assertEquals("testuser", result.getUserName());
        assertEquals(BASIC_STATE, result.getState());
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserDAO).save(captor.capture());
        assertEquals(12345L, captor.getValue().getTelegramUserId());
    }
}
