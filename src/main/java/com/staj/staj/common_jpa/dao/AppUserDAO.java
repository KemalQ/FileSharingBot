package com.staj.staj.common_jpa.dao;

import com.staj.staj.common_jpa.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserDAO extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByTelegramUserId(Long id);//check user in DB
    Optional<AppUser> findById(Long id);
    Optional<AppUser> findByEmail(String email);
}
