package ru.panic.tgdispatchbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.panic.tgdispatchbot.model.Group;
import ru.panic.tgdispatchbot.repository.GroupRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    @Transactional
    public Group create(Group newGroup) {
        return groupRepository.save(newGroup);
    }

    public Optional<Group> getByTelegramChatId(long telegramChatId) {
        return groupRepository.findByTelegramChatId(telegramChatId);
    }

    @Transactional
    public void deleteByTelegramChatId(long telegramChatId) {
        groupRepository.deleteByTelegramChatId(telegramChatId);
    }
}
