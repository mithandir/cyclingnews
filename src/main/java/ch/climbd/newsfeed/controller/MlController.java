package ch.climbd.newsfeed.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Controller;

@Controller
public class MlController {

    private final ChatClient chatClient;

    MlController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String summarize(String text) {
        return this.chatClient.prompt()
                .system("You are a news reporter that summarizes news articles.")
                .user("Summarize the following text: " + text)
                .call()
                .content();
    }

}
