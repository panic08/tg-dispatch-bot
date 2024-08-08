package ru.panic.tgdispatchbot.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "groups_table")
@Builder
public class Group {
    @Id
    private Long id;

    @Column("telegram_chat_id")
    private Long telegramChatId;
}
