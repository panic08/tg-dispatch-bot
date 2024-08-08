package ru.panic.tgdispatchbot.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pair<T, L> {
    private T firstValue;
    private L secondValue;
}
