package com.company;

import kotlin.Unit;
import tomon.bot.Bot;
import tomon.bot.model.Packet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {
    static Bot bot = new Bot();
    
    public static void main(String[] args) throws IOException {
        String current = new java.io.File( "." ).getCanonicalPath();
        System.out.println("Current dir:"+current);
        bot.startWithPassward("name#0000", "password");
        bot.on("DISPATCH", Main::pic);
    }


    private static Unit pic(String event, Object data) {
        if (!event.equals("DISPATCH")) {
            System.out.println("wrong event");
            return Unit.INSTANCE;
        } else {
            Packet message = (Packet)data;
            if (message.getEvent().equals("MESSAGE_CREATE")) {
                if(!message.getData().get("author").getAsJsonObject().get("id").getAsString().equals(bot.id())) { // 查看消息发送者是否为该bot
                    if(message.getData().get("content").getAsString().equals("/pic".trim())) {
                        String channelId = message.getData().get("channel_id").getAsString();
                        ArrayList files = new ArrayList<File>();
                        files.add(new File("./resources/1.jpg"));
                        Map payload = new HashMap<String, String>();
                        payload.put("content", "好看吗");
                        bot.api().route(String.format("/channels/%s/messages", channelId)).post(null, payload, files);
                    }
                }
            }
        }
        return Unit.INSTANCE;
    }
}
