package ru.panic.tgdispatchbot.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "admins_table")
@Builder
public class Admin {
    @Id
    private Long id;

    @Column("telegram_user_id")
    private Long telegramUserId;
}
