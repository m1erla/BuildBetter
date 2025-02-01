package com.renovatipoint.core.utilities.events;

import com.renovatipoint.entities.concretes.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
@Getter
public class ChatMessageSentEvent extends ApplicationEvent {
    private final ChatMessage message;

    public ChatMessageSentEvent(Object source, ChatMessage message){
        super(source);
        this.message = message;
    }
}
