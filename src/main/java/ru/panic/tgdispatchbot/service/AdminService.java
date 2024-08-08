package ru.panic.tgdispatchbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.panic.tgdispatchbot.model.Admin;
import ru.panic.tgdispatchbot.repository.AdminRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    public Optional<Admin> getByTelegramUserId(long telegramUserId) {
        return adminRepository.findByTelegramUserId(telegramUserId);
    }

    @Transactional
    public Admin create(Admin newAdmin) {
        return adminRepository.save(newAdmin);
    }

    @Transactional
    public void deleteByTelegramUserId(long telegramUserId) {
        adminRepository.deleteByTelegramUserId(telegramUserId);
    }

}
