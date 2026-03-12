package com.staj.staj.Node.service;


import com.staj.staj.Node.service.impl.UserActivationServiceImpl;
import com.staj.staj.commonUtils.utils.CryptoTool;
import com.staj.staj.common_jpa.dao.AppUserDAO;
import com.staj.staj.common_jpa.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivationServiceImplTest {

    @Mock
    private AppUserDAO appUserDAO;

    @Mock
    private CryptoTool cryptoTool;

    @InjectMocks
    private UserActivationServiceImpl userActivationService;

    @Test
    void activation_shouldActivateUser_whenUserExists() {
        String cryptoId = "abc123";
        Long userId = 1L;
        AppUser user = new AppUser();
        user.setId(userId);
        user.setIsActive(false);

        when(cryptoTool.idOf(cryptoId)).thenReturn(userId);
        when(appUserDAO.findById(userId)).thenReturn(Optional.of(user));
        when(appUserDAO.save(any(AppUser.class))).thenReturn(user);
        boolean result = userActivationService.activation(cryptoId);
        assertThat(result).isTrue();
        assertThat(user.getIsActive()).isTrue();
        verify(appUserDAO).save(user);
    }

    @Test
    void activation_shouldReturnFalse_whenUserNotExists() {
        String cryptoId = "xyz789";
        Long userId = 2L;
        when(cryptoTool.idOf(cryptoId)).thenReturn(userId);
        when(appUserDAO.findById(userId)).thenReturn(Optional.empty());
        boolean result = userActivationService.activation(cryptoId);
        assertThat(result).isFalse();
        verify(appUserDAO, never()).save(any());
    }
}