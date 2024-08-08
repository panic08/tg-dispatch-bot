package ru.panic.tgdispatchbot.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.panic.tgdispatchbot.model.Button;

import java.util.Collection;

@Repository
public interface ButtonRepository extends CrudRepository<Button, Long> {
    @Query("SELECT b.* FROM buttons_table b")
    Collection<Button> findAll();

    @Query("DELETE FROM buttons_table WHERE id = :id")
    @Modifying
    void deleteById(@Param("id") long id);
}
