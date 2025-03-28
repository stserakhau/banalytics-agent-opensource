package com.banalytics.box.module.telegram.handlers;

import com.banalytics.box.module.AbstractAction;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.ExecutionContext;
import com.banalytics.box.module.IAction;
import com.banalytics.box.service.SystemThreadsService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.banalytics.box.module.telegram.handlers.HomeCommandHandler.COMMAND_HOME;

@Slf4j
public class BrowseActionsCommandHandler extends AbstractCommandHandler {
    public final static String COMMAND_BROWSE_ACTIONS = "/Actions";

    private final BoxEngine engine;

    public BrowseActionsCommandHandler(TelegramBot bot, BoxEngine engine) {
        super(bot);
        this.engine = engine;
    }

    @Override
    public String getCommand() {
        return COMMAND_BROWSE_ACTIONS;
    }

    private final Map<String, List<AbstractAction<?>>> actionGroups = new HashMap<>(); // group title -> List[Action,...]
    private final Map<Long, LinkedList<String>> chatSelectedPath = new HashMap<>(); // chatId -> path in action group

    @Override
    public void handle(long chatId) {
        actionGroups.clear();
        chatSelectedPath.remove(chatId);
        Collection<AbstractAction<?>> actions = engine.findActionTasks();

        Map<String, String> locale = engine.i18n().get("en");

        for (AbstractAction<?> action : actions) {
            String title = locale.get(action.getSelfClassName());
            if (title == null) {
                title = action.getSelfClassName();
            }
            List<AbstractAction<?>> groupActions = actionGroups.computeIfAbsent(title, a -> new ArrayList<>());
            groupActions.add(action);
        }


        int colsPerRow = 2;

        ReplyKeyboardMarkup keyb = new ReplyKeyboardMarkup(new KeyboardButton(COMMAND_HOME));
        List<KeyboardButton> row = new ArrayList<>();

        List<String> actionGroupNames = new ArrayList<>(actionGroups.keySet());
        actionGroupNames.sort(String::compareTo);
        for (String actionGroup : actionGroupNames) {
            if (row.size() == colsPerRow) {
                keyb.addRow(row.toArray(new KeyboardButton[0]));
                row.clear();
            }
            row.add(new KeyboardButton(actionGroup));
        }
        if (!row.isEmpty()) {
            keyb.addRow(row.toArray(new KeyboardButton[0]));
        }

        bot.execute(new SendMessage(chatId, COMMAND_BROWSE_ACTIONS)
                .parseMode(ParseMode.Markdown)
                .disableWebPagePreview(false)
                .replyMarkup(keyb));
    }

    // Play Audio ->
    @Override
    public void handleArgs(long chatId, String... args) {
        final Map<String, String> locale = engine.i18n().get("en");

        String contextPath = args[0];
        LinkedList<String> path = chatSelectedPath.computeIfAbsent(chatId, k -> new LinkedList<>());

        if ("[..]".equals(contextPath) && !path.isEmpty()) {
            path.removeLast();
        }

        List<String> contextItems = new ArrayList<>();
        switch (path.size()) {
            case 0 -> { //on select action group
                path.add(contextPath);
                List<AbstractAction<?>> actions = actionGroups.get(contextPath);
                for (AbstractAction<?> action : actions) {
                    String title = locale.get(action.getSelfClassName());
                    if (title == null) {
                        title = action.getSelfClassName();
                    }
                    title = title + ": " + action.getTitle();

                    contextItems.add(title);
                }
                ReplyKeyboardMarkup keyb;
                if (path.size() > 1) {
                    keyb = new ReplyKeyboardMarkup(new KeyboardButton(COMMAND_HOME), new KeyboardButton("[..]"));
                } else {
                    keyb = new ReplyKeyboardMarkup(new KeyboardButton(COMMAND_HOME), new KeyboardButton(COMMAND_BROWSE_ACTIONS));
                }
                List<KeyboardButton> row = new ArrayList<>();

                contextItems.sort(String::compareTo);
                int colsPerRow = 1;
                for (String contextItem : contextItems) {
                    if (row.size() == colsPerRow) {
                        keyb.addRow(row.toArray(new KeyboardButton[0]));
                        row.clear();
                    }
                    row.add(new KeyboardButton(contextItem));
                }
                if (!row.isEmpty()) {
                    keyb.addRow(row.toArray(new KeyboardButton[0]));
                }
                bot.execute(new SendMessage(chatId, contextPath)
                        .parseMode(ParseMode.Markdown)
                        .disableWebPagePreview(false)
                        .replyMarkup(keyb)
                );
            }
            case 1 -> {// on select action to execution
                List<AbstractAction<?>> actions = actionGroups.get(path.get(0));
                for (AbstractAction<?> action : actions) {
                    String title = locale.get(action.getSelfClassName());
                    if (title == null) {
                        title = action.getSelfClassName();
                    }
                    String actionTitle = action.getTitle();
                    String i18nTitle = locale.get(actionTitle);
                    if (i18nTitle == null) {
                        i18nTitle = actionTitle;
                    }
                    title = title + ": " + i18nTitle;

                    if (title.equals(contextPath)) {
                        SystemThreadsService.execute(this, () -> {
                            try {
                                ExecutionContext ctx = new ExecutionContext();
                                ctx.setVar(IAction.MANUAL_RUN, IAction.MANUAL_RUN);
                                action.action(ctx);
                                bot.execute(new SendMessage(chatId, contextPath + " executed")
                                        .parseMode(ParseMode.Markdown)
                                );
                            } catch (Exception e) {
                                log.error("Fail to execute action: " + path, e);
                            }
                        });
                        break;
                    }
                }
            }
            default -> {
                System.out.println("def");
            }
        }
//        bot.execute(new SendMessage(chatId, selectedActionGroup + " unavailable"));

    }
}
