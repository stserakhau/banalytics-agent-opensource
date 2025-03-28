package com.banalytics.box.module.telegram.handlers;

import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.InitShutdownSupport;
import com.banalytics.box.module.MediaCaptureCallbackSupport;
import com.banalytics.box.module.Thing;
import com.banalytics.box.module.standard.LocalMediaStream;
import com.banalytics.box.module.standard.Onvif;
import com.banalytics.box.module.standard.UrlMediaStream;
import com.pengrad.telegrambot.TelegramBot;

import java.util.List;

public class VideoShotAllCommandHandler extends AbstractCommandHandler {
    public final static String COMMAND_VIDEO_SHOT_ALL = "/Video Shot All";

    private final BoxEngine engine;

    public VideoShotAllCommandHandler(TelegramBot bot, BoxEngine engine) {
        super(bot);
        this.engine = engine;
    }

    @Override
    public String getCommand() {
        return COMMAND_VIDEO_SHOT_ALL;
    }

    @Override
    public void handle(long chatId) {
        List<Thing<?>> things = engine.findThingsByStandard(Onvif.class, UrlMediaStream.class, LocalMediaStream.class);
        for (Thing<?> thing : things) {
            for (InitShutdownSupport subscriber : thing.getSubscribers()) {
                if (subscriber instanceof MediaCaptureCallbackSupport mcb) {    // first is always grabber
                    mcb.screenShot(
                            mediaResult -> sendMediaResult(thing.getTitle(), chatId, mediaResult)
                    );
                    break;                                                      // then break and go to next
                }
            }
        }
    }
}
