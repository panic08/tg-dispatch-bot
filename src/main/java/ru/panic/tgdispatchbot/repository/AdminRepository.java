package ru.panic.tgdispatchbot.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.panic.tgdispatchbot.model.Admin;

import java.util.Optional;

@Repository
public interface AdminRepository extends CrudRepository<Admin, Long> {
    @Query("SELECT a.* FROM admins_table a where a.telegram_user_id = :telegramUserId")
    Optional<Admin> findByTelegramUserId(@Param("telegramUserId") long telegramUserId);

    @Query("DELETE FROM admins_table WHERE telegram_user_id = :telegramUserId")
    @Modifying
    void deleteByTelegramUserId(@Param("telegramUserId") long telegramUserId);
}
