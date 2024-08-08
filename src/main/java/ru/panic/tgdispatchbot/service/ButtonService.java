package ru.panic.tgdispatchbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.panic.tgdispatchbot.model.Button;
import ru.panic.tgdispatchbot.repository.ButtonRepository;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class ButtonService {

    private final ButtonRepository buttonRepository;

    @Transactional
    public Button create(Button newButton) {
        return buttonRepository.save(newButton);
    }

    public Collection<Button> getAll() {
        return buttonRepository.findAll();
    }

    @Transactional
    public void deleteById(long id) {
        buttonRepository.deleteById(id);
    }
}
