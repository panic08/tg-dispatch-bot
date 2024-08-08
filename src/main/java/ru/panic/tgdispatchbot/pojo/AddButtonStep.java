package ru.panic.tgdispatchbot.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AddButtonStep {
    private int stage;
    private int oldMessageId;
    private String text;
}
