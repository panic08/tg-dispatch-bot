package ru.panic.tgdispatchbot.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.panic.tgdispatchbot.model.Group;

import java.util.Optional;

@Repository
public interface GroupRepository extends CrudRepository<Group, Long> {
    @Query("SELECT g.* FROM groups_table g WHERE g.telegram_chat_id = :telegramChatId")
    Optional<Group> findByTelegramChatId(@Param("telegramChatId") long telegramChatId);

    @Query("DELETE FROM groups_table WHERE telegram_chat_id = :telegramChatId")
    @Modifying
    void deleteByTelegramChatId(@Param("telegramChatId") long telegramChatId);
}
