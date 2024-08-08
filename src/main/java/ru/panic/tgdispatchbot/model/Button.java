package ru.panic.tgdispatchbot.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@Table(name = "buttons_table")
public class Button {
    @Id
    private Long id;

    private String text;

    private String link;
}
