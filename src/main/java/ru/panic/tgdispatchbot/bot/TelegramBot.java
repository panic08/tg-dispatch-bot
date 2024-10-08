package ru.panic.tgdispatchbot.bot;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.games.Animation;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.panic.tgdispatchbot.bot.callback.AdminCallback;
import ru.panic.tgdispatchbot.model.Admin;
import ru.panic.tgdispatchbot.model.Button;
import ru.panic.tgdispatchbot.model.Group;
import ru.panic.tgdispatchbot.pojo.AddButtonStep;
import ru.panic.tgdispatchbot.property.TelegramBotProperty;
import ru.panic.tgdispatchbot.service.AdminService;
import ru.panic.tgdispatchbot.service.ButtonService;
import ru.panic.tgdispatchbot.service.GroupService;
import ru.panic.tgdispatchbot.util.URLFileDownloaderUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    @PostConstruct
    public void init() {
        List<BotCommand> listOfCommands = new ArrayList<>();

        listOfCommands.add(new BotCommand("/admin", "\uD83D\uDD34 Admin panel"));

        try {
            execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
    }

    private final TelegramBotProperty telegramBotProperty;
    private final AdminService adminService;
    private final GroupService groupService;
    private final ButtonService buttonService;
    private final URLFileDownloaderUtil urlFileDownloaderUtil;

    private final Map<Long, Integer> addAdminSteps = new HashMap<>();
    private final Map<Long, Integer> deleteAdminSteps = new HashMap<>();
    private final Map<Long, Integer> writeAnnouncementSteps = new HashMap<>();
    private final Map<Long, Integer> addGroupSteps = new HashMap<>();
    private final Map<Long, Integer> deleteGroupSteps = new HashMap<>();
    private final Map<Long, AddButtonStep> addButtonSteps = new HashMap<>();

    @Override
    public String getBotUsername() {
        return "empty";
    }

    @Override
    public String getBotToken() {
        return telegramBotProperty.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        new Thread(() -> {
            // Message handler
            if (update.hasMessage()) {
                long telegramUserId = update.getMessage().getFrom().getId();
                long chatId = update.getMessage().getChatId();
                int messageId = update.getMessage().getMessageId();

                if (chatId != telegramUserId) {
                    return;
                }

                Optional<Admin> currentAdmin = adminService.getByTelegramUserId(telegramUserId);

                // handle current user is not admin
                if (currentAdmin.isEmpty()) {
                    handleSendMessage(SendMessage.builder()
                            .chatId(chatId)
                            .text("⛔\uFE0F <b>You are not an administrator</b>\n\n"
                                    + "In order to use the admin panel features, you must be given admin rights. Your ID: <code>" + telegramUserId + "</code>")
                            .parseMode("html")
                            .build());
                    return;
                }

                // handle text message
                if (update.getMessage().hasText()) {
                    String text = update.getMessage().getText();

                    //back to admin inlineKeyboardMarkup
                    InlineKeyboardMarkup backToAdminKeyboardMarkup = new InlineKeyboardMarkup();

                    InlineKeyboardButton backToAdmin = InlineKeyboardButton.builder()
                            .callbackData(AdminCallback.BACK_TO_ADMIN_CALLBACK)
                            .text("↩\uFE0F Back")
                            .build();

                    List<InlineKeyboardButton> backToAdminKeyboardRows = new ArrayList<>();

                    backToAdminKeyboardRows.add(backToAdmin);

                    List<List<InlineKeyboardButton>> backToAdminRowList = new ArrayList<>();

                    backToAdminRowList.add(backToAdminKeyboardRows);

                    backToAdminKeyboardMarkup.setKeyboard(backToAdminRowList);

                    // handle steps operations
                    if (addAdminSteps.containsKey(telegramUserId)) {
                        // delete current message
                        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                        long newAdminTelegramUserId = Long.parseLong(text);

                        int oldMessageId = addAdminSteps.get(telegramUserId);

                        // remove this telegram user from addAdminSteps
                        addAdminSteps.remove(telegramUserId);

                        if (adminService.getByTelegramUserId(newAdminTelegramUserId).isPresent()) {

                            handleEditMessageText(EditMessageText.builder()
                                    .chatId(chatId)
                                    .messageId(oldMessageId)
                                    .text("❌ <b>Add admin</b>\n\n"
                                            + "An admin with that ID already exists")
                                    .replyMarkup(backToAdminKeyboardMarkup)
                                    .parseMode("html")
                                    .build());
                            return;
                        }

                        // add a new admin
                        adminService.create(Admin.builder().telegramUserId(newAdminTelegramUserId).build());

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(oldMessageId)
                                .text("✅ <b>Add admin</b>\n\n"
                                        + "You have successfully added a new admin")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());

                        return;
                    }

                    if (deleteAdminSteps.containsKey(telegramUserId)) {
                        // delete current message
                        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                        long newAdminTelegramUserId = Long.parseLong(text);

                        int oldMessageId = deleteAdminSteps.get(telegramUserId);

                        // remove this telegram user from addAdminSteps
                        deleteAdminSteps.remove(telegramUserId);

                        // delete the admin
                        adminService.deleteByTelegramUserId(newAdminTelegramUserId);

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(oldMessageId)
                                .text("✅ <b>Delete admin</b>\n\n"
                                        + "You have successfully deleted the admin")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());

                        return;
                    }

                    if (addGroupSteps.containsKey(telegramUserId)) {
                        // delete current message
                        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                        long newGroupChatId = Long.parseLong(text);

                        int oldMessageId = addGroupSteps.get(telegramUserId);

                        // remove this telegram user from addAdminSteps
                        addGroupSteps.remove(telegramUserId);

                        if (groupService.getByTelegramChatId(newGroupChatId).isPresent()) {
                            handleEditMessageText(EditMessageText.builder()
                                    .chatId(chatId)
                                    .messageId(oldMessageId)
                                    .text("❌ <b>Add group</b>\n\n"
                                            + "A group with that CHAT-ID already exists")
                                    .replyMarkup(backToAdminKeyboardMarkup)
                                    .parseMode("html")
                                    .build());
                            return;
                        }

                        // add a new group
                        groupService.create(Group.builder().telegramChatId(newGroupChatId).build());

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(oldMessageId)
                                .text("✅ <b>Add group</b>\n\n"
                                        + "You have successfully added a new group")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());

                        return;
                    }

                    if (deleteGroupSteps.containsKey(telegramUserId)) {
                        // delete current message
                        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                        long groupChatId = Long.parseLong(text);

                        int oldMessageId = deleteGroupSteps.get(telegramUserId);

                        // remove this telegram user from addAdminSteps
                        deleteGroupSteps.remove(telegramUserId);

                        // delete the group
                        groupService.deleteByTelegramChatId(groupChatId);

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(oldMessageId)
                                .text("✅ <b>Delete group</b>\n\n"
                                        + "You have successfully deleted the group")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());

                        return;
                    }

                    if (addButtonSteps.containsKey(telegramUserId)) {
                        // delete current message
                        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                        // get addButtonStep
                        AddButtonStep addButtonStep = addButtonSteps.get(telegramUserId);

                        switch (addButtonStep.getStage()) {
                            case 1 -> {
                                addButtonStep.setStage(2);
                                addButtonStep.setText(text);

                                handleEditMessageText(EditMessageText.builder()
                                        .chatId(chatId)
                                        .messageId(addButtonStep.getOldMessageId())
                                        .text("➕ Add button\n\n"
                                                + "Enter the link for the button to continue")
                                        .parseMode("html")
                                        .replyMarkup(backToAdminKeyboardMarkup)
                                        .build());
                            }

                            case 2 -> {
                                //delete step
                                addButtonSteps.remove(telegramUserId);

                                //create a new button
                                buttonService.create(Button.builder().text(addButtonStep.getText()).link(text).build());

                                handleEditMessageText(EditMessageText.builder()
                                        .chatId(chatId)
                                        .messageId(addButtonStep.getOldMessageId())
                                        .text("✅ Add button\n\n"
                                                + "You have successfully created a new button")
                                        .parseMode("html")
                                        .replyMarkup(backToAdminKeyboardMarkup)
                                        .build());
                            }
                        }
                        return;
                    }


                    switch (text) {
                        case "/start", "/admin" -> {
                            // handle admin panel message
                            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                            InlineKeyboardButton addButtonButton = InlineKeyboardButton.builder()
                                    .callbackData(AdminCallback.ADD_BUTTON_CALLBACK)
                                    .text("➕ Add button")
                                    .build();

                            InlineKeyboardButton deleteButtonButton = InlineKeyboardButton.builder()
                                    .callbackData(AdminCallback.DELETE_BUTTON_CALLBACK)
                                    .text("➖ Delete button")
                                    .build();

                            List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();

                            keyboardButtonsRow3.add(addButtonButton);
                            keyboardButtonsRow3.add(deleteButtonButton);

                            List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

                            if (telegramBotProperty.getFullAdmins().contains(telegramUserId)) {
                                List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
                                List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();

                                InlineKeyboardButton addAdminButton = InlineKeyboardButton.builder()
                                        .callbackData(AdminCallback.ADD_ADMIN_CALLBACK)
                                        .text("➕ Add admin")
                                        .build();

                                InlineKeyboardButton deleteAdminButton = InlineKeyboardButton.builder()
                                        .callbackData(AdminCallback.DELETE_ADMIN_CALLBACK)
                                        .text("➖ Delete admin")
                                        .build();

                                InlineKeyboardButton addGroupButton = InlineKeyboardButton.builder()
                                        .callbackData(AdminCallback.ADD_GROUP_CALLBACK)
                                        .text("➕ Add group")
                                        .build();

                                InlineKeyboardButton deleteGroupButton = InlineKeyboardButton.builder()
                                        .callbackData(AdminCallback.DELETE_GROUP_CALLBACK)
                                        .text("➖ Delete group")
                                        .build();

                                keyboardButtonsRow1.add(addAdminButton);
                                keyboardButtonsRow1.add(deleteAdminButton);

                                keyboardButtonsRow2.add(addGroupButton);
                                keyboardButtonsRow2.add(deleteGroupButton);

                                rowList.add(keyboardButtonsRow1);
                                rowList.add(keyboardButtonsRow2);
                            }

                            rowList.add(keyboardButtonsRow3);

                            inlineKeyboardMarkup.setKeyboard(rowList);

                            handleSendMessage(SendMessage.builder()
                                    .chatId(chatId)
                                    .text("\uD83D\uDCDB <b>Admin panel</b>")
                                    .parseMode("html")
                                    .replyMarkup(inlineKeyboardMarkup)
                                    .build());
                            return;
                        }
                    }

                    // other reaction

                    // delete current message
                    handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                    //delete step
                    writeAnnouncementSteps.remove(telegramUserId);

                    handleSendMessage(SendMessage.builder()
                            .chatId(chatId)
                            .text("✅ <b>Write announcement</b>\n\n"
                                    + "You have successfully forwarded your message to the groups linked to the bot")
                            .parseMode("html")
                            .build());

                    //get all groups
                    Collection<Group> allLinkedGroups = groupService.getAll();

                    Collection<Button> allButtons = buttonService.getAll();

                    InlineKeyboardMarkup newInlineKeyboardMarkup = new InlineKeyboardMarkup();

                    List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

                    for (Button button : allButtons) {
                        List<InlineKeyboardButton> newKeyboardButtonsRow = new ArrayList<>();

                        InlineKeyboardButton newButton = InlineKeyboardButton.builder()
                                .text(button.getText())
                                .url(button.getLink())
                                .build();

                        newKeyboardButtonsRow.add(newButton);

                        rowList.add(newKeyboardButtonsRow);
                    }

                    newInlineKeyboardMarkup.setKeyboard(rowList);

                    for (Group linkedGroup : allLinkedGroups) {
                        handleSendMessage(SendMessage.builder()
                                .chatId(linkedGroup.getTelegramChatId())
                                .text("<b>" + text + "</b>")
                                .replyMarkup(newInlineKeyboardMarkup)
                                .parseMode("html")
                                .build());
                    }

                } else if (update.getMessage().hasPhoto()) {
                    PhotoSize photo = update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1);
                    String caption = update.getMessage().getCaption();

                    //back to admin inlineKeyboardMarkup
                    InlineKeyboardMarkup backToAdminKeyboardMarkup = new InlineKeyboardMarkup();

                    InlineKeyboardButton backToAdmin = InlineKeyboardButton.builder()
                            .callbackData(AdminCallback.BACK_TO_ADMIN_CALLBACK)
                            .text("↩\uFE0F Back")
                            .build();

                    List<InlineKeyboardButton> backToAdminKeyboardRows = new ArrayList<>();

                    backToAdminKeyboardRows.add(backToAdmin);

                    List<List<InlineKeyboardButton>> backToAdminRowList = new ArrayList<>();

                    backToAdminRowList.add(backToAdminKeyboardRows);

                    backToAdminKeyboardMarkup.setKeyboard(backToAdminRowList);

                    //write announcement step

                    // delete current message
                    handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                    //delete step
                    writeAnnouncementSteps.remove(telegramUserId);

                    handleSendMessage(SendMessage.builder()
                            .chatId(chatId)
                            .text("✅ <b>Write announcement</b>\n\n"
                                    + "You have successfully forwarded your message to the groups linked to the bot")
                            .parseMode("html")
                            .build());

                    File mediaFile = null;

                    //---- download media file

                    try {
                        String URLWithMediaFile = execute(GetFile.builder().fileId(photo.getFileId()).build()).getFileUrl(telegramBotProperty.getToken());

                        mediaFile = urlFileDownloaderUtil.downloadFileAndPackInTemp(URLWithMediaFile, "photo" + UUID.randomUUID(), ".png");

                    } catch (TelegramApiException | IOException e) {
                        log.warn(e.getMessage());
                    }
                    //---

                    //get all groups
                    Collection<Group> allLinkedGroups = groupService.getAll();

                    Collection<Button> allButtons = buttonService.getAll();

                    InlineKeyboardMarkup newInlineKeyboardMarkup = new InlineKeyboardMarkup();

                    List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

                    for (Button button : allButtons) {
                        List<InlineKeyboardButton> newKeyboardButtonsRow = new ArrayList<>();

                        InlineKeyboardButton newButton = InlineKeyboardButton.builder()
                                .text(button.getText())
                                .url(button.getLink())
                                .build();

                        newKeyboardButtonsRow.add(newButton);

                        rowList.add(newKeyboardButtonsRow);
                    }

                    newInlineKeyboardMarkup.setKeyboard(rowList);

                    InputFile inputMediaFile = new InputFile(mediaFile);

                    if (caption == null) {
                        caption = "";
                    }

                    for (Group linkedGroup : allLinkedGroups) {
                        handleSendPhoto(SendPhoto.builder()
                                .chatId(linkedGroup.getTelegramChatId())
                                .caption("<b>" + caption + "</b>")
                                .photo(inputMediaFile)
                                .parseMode("html")
                                .replyMarkup(newInlineKeyboardMarkup)
                                .build());
                    }

                    // resource release
                    mediaFile.delete();
                } else if (update.getMessage().hasAnimation()) {
                    Animation animation = update.getMessage().getAnimation();
                    String caption = update.getMessage().getCaption();

                    //back to admin inlineKeyboardMarkup
                    InlineKeyboardMarkup backToAdminKeyboardMarkup = new InlineKeyboardMarkup();

                    InlineKeyboardButton backToAdmin = InlineKeyboardButton.builder()
                            .callbackData(AdminCallback.BACK_TO_ADMIN_CALLBACK)
                            .text("↩\uFE0F Back")
                            .build();

                    List<InlineKeyboardButton> backToAdminKeyboardRows = new ArrayList<>();

                    backToAdminKeyboardRows.add(backToAdmin);

                    List<List<InlineKeyboardButton>> backToAdminRowList = new ArrayList<>();

                    backToAdminRowList.add(backToAdminKeyboardRows);

                    backToAdminKeyboardMarkup.setKeyboard(backToAdminRowList);

                    //write announcement step

                    // delete current message
                    handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                    //delete step
                    writeAnnouncementSteps.remove(telegramUserId);

                    handleSendMessage(SendMessage.builder()
                            .chatId(chatId)
                            .text("✅ <b>Write announcement</b>\n\n"
                                    + "You have successfully forwarded your message to the groups linked to the bot")
                            .parseMode("html")
                            .build());

                    File mediaFile = null;

                    //---- download media file

                    try {
                        String URLWithMediaFile = execute(GetFile.builder().fileId(animation.getFileId()).build()).getFileUrl(telegramBotProperty.getToken());

                        mediaFile = urlFileDownloaderUtil.downloadFileAndPackInTemp(URLWithMediaFile, "gif" + UUID.randomUUID(), ".gif");

                    } catch (TelegramApiException | IOException e) {
                        log.warn(e.getMessage());
                    }
                    //---

                    //get all groups
                    Collection<Group> allLinkedGroups = groupService.getAll();

                    Collection<Button> allButtons = buttonService.getAll();

                    InlineKeyboardMarkup newInlineKeyboardMarkup = new InlineKeyboardMarkup();

                    List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

                    for (Button button : allButtons) {
                        List<InlineKeyboardButton> newKeyboardButtonsRow = new ArrayList<>();

                        InlineKeyboardButton newButton = InlineKeyboardButton.builder()
                                .text(button.getText())
                                .url(button.getLink())
                                .build();

                        newKeyboardButtonsRow.add(newButton);

                        rowList.add(newKeyboardButtonsRow);
                    }

                    newInlineKeyboardMarkup.setKeyboard(rowList);

                    InputFile inputMediaFile = new InputFile(mediaFile);

                    if (caption == null) {
                        caption = "";
                    }

                    for (Group linkedGroup : allLinkedGroups) {
                        handleSendAnimation(SendAnimation.builder()
                                .chatId(linkedGroup.getTelegramChatId())
                                .caption("<b>" + caption + "</b>")
                                .animation(inputMediaFile)
                                .parseMode("html")
                                .replyMarkup(newInlineKeyboardMarkup)
                                .build());
                    }

                    // resource release
                    mediaFile.delete();
                } else if (update.getMessage().hasVideo()) {
                    Video video = update.getMessage().getVideo();
                    String caption = update.getMessage().getCaption();

                    //back to admin inlineKeyboardMarkup
                    InlineKeyboardMarkup backToAdminKeyboardMarkup = new InlineKeyboardMarkup();

                    InlineKeyboardButton backToAdmin = InlineKeyboardButton.builder()
                            .callbackData(AdminCallback.BACK_TO_ADMIN_CALLBACK)
                            .text("↩\uFE0F Back")
                            .build();

                    List<InlineKeyboardButton> backToAdminKeyboardRows = new ArrayList<>();

                    backToAdminKeyboardRows.add(backToAdmin);

                    List<List<InlineKeyboardButton>> backToAdminRowList = new ArrayList<>();

                    backToAdminRowList.add(backToAdminKeyboardRows);

                    backToAdminKeyboardMarkup.setKeyboard(backToAdminRowList);

                    //write announcement step
                    // delete current message
                    handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                    //delete step
                    writeAnnouncementSteps.remove(telegramUserId);

                    handleSendMessage(SendMessage.builder()
                            .chatId(chatId)
                            .text("✅ <b>Write announcement</b>\n\n"
                                    + "You have successfully forwarded your message to the groups linked to the bot")
                            .parseMode("html")
                            .build());

                    File mediaFile = null;

                    //---- download media file

                    try {
                        String URLWithMediaFile = execute(GetFile.builder().fileId(video.getFileId()).build()).getFileUrl(telegramBotProperty.getToken());

                        mediaFile = urlFileDownloaderUtil.downloadFileAndPackInTemp(URLWithMediaFile, "video" + UUID.randomUUID(), ".mp4");

                    } catch (TelegramApiException | IOException e) {
                        log.warn(e.getMessage());
                    }
                    //---

                    //get all groups
                    Collection<Group> allLinkedGroups = groupService.getAll();

                    Collection<Button> allButtons = buttonService.getAll();

                    InlineKeyboardMarkup newInlineKeyboardMarkup = new InlineKeyboardMarkup();

                    List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

                    for (Button button : allButtons) {
                        List<InlineKeyboardButton> newKeyboardButtonsRow = new ArrayList<>();

                        InlineKeyboardButton newButton = InlineKeyboardButton.builder()
                                .text(button.getText())
                                .url(button.getLink())
                                .build();

                        newKeyboardButtonsRow.add(newButton);

                        rowList.add(newKeyboardButtonsRow);
                    }

                    newInlineKeyboardMarkup.setKeyboard(rowList);

                    InputFile inputMediaFile = new InputFile(mediaFile);

                    if (caption == null) {
                        caption = "";
                    }

                    for (Group linkedGroup : allLinkedGroups) {
                        handleSendVideo(SendVideo.builder()
                                .chatId(linkedGroup.getTelegramChatId())
                                .caption("<b>" + caption + "</b>")
                                .video(inputMediaFile)
                                .parseMode("html")
                                .replyMarkup(newInlineKeyboardMarkup)
                                .build());
                    }

                    // resource release
                    mediaFile.delete();
                }
            } else if (update.hasCallbackQuery()) {
                String callbackQueryData = update.getCallbackQuery().getData();
                int messageId = update.getCallbackQuery().getMessage().getMessageId();
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                long telegramUserId = update.getCallbackQuery().getFrom().getId();
                String callbackQueryId = update.getCallbackQuery().getId();

                Optional<Admin> currentAdmin = adminService.getByTelegramUserId(telegramUserId);

                // handle current user is not admin
                if (currentAdmin.isEmpty()) {
                    handleAnswerCallbackQuery(AnswerCallbackQuery.builder().text("You don't have the admin rights")
                            .callbackQueryId(callbackQueryId).build());
                    return;
                }

                //back to admin inlineKeyboardMarkup
                InlineKeyboardMarkup backToAdminKeyboardMarkup = new InlineKeyboardMarkup();

                InlineKeyboardButton backToAdmin = InlineKeyboardButton.builder()
                        .callbackData(AdminCallback.BACK_TO_ADMIN_CALLBACK)
                        .text("↩\uFE0F Back")
                        .build();

                List<InlineKeyboardButton> backToAdminKeyboardRows = new ArrayList<>();

                backToAdminKeyboardRows.add(backToAdmin);

                List<List<InlineKeyboardButton>> backToAdminRowList = new ArrayList<>();

                backToAdminRowList.add(backToAdminKeyboardRows);

                backToAdminKeyboardMarkup.setKeyboard(backToAdminRowList);

                switch (callbackQueryData) {
                    // handle add admin message
                    case AdminCallback.ADD_ADMIN_CALLBACK -> {
                        addAdminSteps.put(telegramUserId, messageId);

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .text("➕ <b>Add admin</b>\n\n"
                                        + "Enter the ID of the user you want to add administrator rights")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());
                    }

                    case AdminCallback.DELETE_ADMIN_CALLBACK -> {
                        deleteAdminSteps.put(telegramUserId, messageId);

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .text("➖ <b>Delete admin</b>\n\n"
                                        + "Enter the ID of the user you want to delete administrator rights")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());
                    }

                    case AdminCallback.ADD_GROUP_CALLBACK -> {
                        addGroupSteps.put(telegramUserId, messageId);

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .text("➕ <b>Add group</b>\n\n"
                                        + "Enter the CHAT-ID of the group you want to add")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());
                    }

                    case AdminCallback.DELETE_GROUP_CALLBACK -> {
                        deleteGroupSteps.put(telegramUserId, messageId);

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .text("➖ <b>Delete group</b>\n\n"
                                        + "Enter the CHAT-ID of the group you want to delete")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());
                    }

                    case AdminCallback.ADD_BUTTON_CALLBACK -> {
                        addButtonSteps.put(telegramUserId, new AddButtonStep(1, messageId, null));

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .text("➕ <b>Add button</b>\n\n"
                                        + "Enter text for button to continue")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());
                    }

                    case AdminCallback.DELETE_BUTTON_CALLBACK -> handleDeleteButtonMenu(chatId, messageId);

                    case AdminCallback.WRITE_ANNOUNCEMENT_CALLBACK -> {
                        writeAnnouncementSteps.put(telegramUserId, messageId);

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .text("\uD83D\uDCE3 <b>Write announcement</b>\n\n"
                                + "Send a message that will be forwarded to all groups attached to the bot")
                                .replyMarkup(backToAdminKeyboardMarkup)
                                .parseMode("html")
                                .build());
                    }

                    //handle back to admin
                    case AdminCallback.BACK_TO_ADMIN_CALLBACK -> {
                        addAdminSteps.remove(telegramUserId);
                        deleteAdminSteps.remove(telegramUserId);
                        addGroupSteps.remove(telegramUserId);
                        deleteGroupSteps.remove(telegramUserId);
                        addButtonSteps.remove(telegramUserId);
                        writeAnnouncementSteps.remove(telegramUserId);

                        // handle admin panel message
                        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                        InlineKeyboardButton addButtonButton = InlineKeyboardButton.builder()
                                .callbackData(AdminCallback.ADD_BUTTON_CALLBACK)
                                .text("➕ Add button")
                                .build();

                        InlineKeyboardButton deleteButtonButton = InlineKeyboardButton.builder()
                                .callbackData(AdminCallback.DELETE_BUTTON_CALLBACK)
                                .text("➖ Delete button")
                                .build();

                        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();

                        keyboardButtonsRow3.add(addButtonButton);
                        keyboardButtonsRow3.add(deleteButtonButton);

                        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

                        if (telegramBotProperty.getFullAdmins().contains(telegramUserId)) {
                            List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
                            List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();

                            InlineKeyboardButton addAdminButton = InlineKeyboardButton.builder()
                                    .callbackData(AdminCallback.ADD_ADMIN_CALLBACK)
                                    .text("➕ Add admin")
                                    .build();

                            InlineKeyboardButton deleteAdminButton = InlineKeyboardButton.builder()
                                    .callbackData(AdminCallback.DELETE_ADMIN_CALLBACK)
                                    .text("➖ Delete admin")
                                    .build();

                            InlineKeyboardButton addGroupButton = InlineKeyboardButton.builder()
                                    .callbackData(AdminCallback.ADD_GROUP_CALLBACK)
                                    .text("➕ Add group")
                                    .build();

                            InlineKeyboardButton deleteGroupButton = InlineKeyboardButton.builder()
                                    .callbackData(AdminCallback.DELETE_GROUP_CALLBACK)
                                    .text("➖ Delete group")
                                    .build();

                            keyboardButtonsRow1.add(addAdminButton);
                            keyboardButtonsRow1.add(deleteAdminButton);

                            keyboardButtonsRow2.add(addGroupButton);
                            keyboardButtonsRow2.add(deleteGroupButton);

                            rowList.add(keyboardButtonsRow1);
                            rowList.add(keyboardButtonsRow2);
                        }

                        rowList.add(keyboardButtonsRow3);

                        inlineKeyboardMarkup.setKeyboard(rowList);

                        handleEditMessageText(EditMessageText.builder()
                                .chatId(chatId)
                                .messageId(messageId)
                                .text("\uD83D\uDCDB <b>Admin panel</b>")
                                .parseMode("html")
                                .replyMarkup(inlineKeyboardMarkup)
                                .build());
                    }
                }

                if (callbackQueryData.contains(AdminCallback.DELETE_SPECIFIC_BUTTON_CALLBACK)) {
                    long buttonId = Long.parseLong(callbackQueryData.split(" ")[4]);

                    buttonService.deleteById(buttonId);

                    handleAnswerCallbackQuery(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQueryId)
                            .text("You have successfully deleted the button")
                            .build());

                    handleDeleteButtonMenu(chatId, messageId);
                }
            }
        }).start();
    }

    private void handleDeleteButtonMenu(long chatId, int messageId) {
        Collection<Button> buttons = buttonService.getAll();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        for (Button button : buttons) {
            List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();

            keyboardButtonsRow.add(InlineKeyboardButton.builder()
                    .text(button.getText() + " | " + button.getLink())
                    .callbackData(AdminCallback.DELETE_SPECIFIC_BUTTON_CALLBACK + " " + button.getId())
                    .build());

            rowList.add(keyboardButtonsRow);
        }

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        InlineKeyboardButton backToAdminButton = InlineKeyboardButton.builder()
                .callbackData(AdminCallback.BACK_TO_ADMIN_CALLBACK)
                .text("↩\uFE0F Back")
                .build();

        keyboardButtonsRow1.add(backToAdminButton);

        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        handleEditMessageText(EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("➖ <b>Delete button</b>\n\n"
                        + "Select the button you want to delete")
                .replyMarkup(inlineKeyboardMarkup)
                .parseMode("html")
                .build());
    }

    private Message handleSendMessage(SendMessage sendMessage) {
        try {
            return execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private Message handleSendPhoto(SendPhoto sendPhoto) {
        try {
            return execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private Message handleSendAnimation(SendAnimation sendAnimation) {
        try {
            return execute(sendAnimation);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private Message handleSendVideo(SendVideo sendVideo) {
        try {
            return execute(sendVideo);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private void handleAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        try {
            execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void handleEditMessageText(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void handleDeleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
