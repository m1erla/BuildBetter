package com.buildbetter.core.utilities.events;

import com.buildbetter.entities.concretes.Request;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RequestStatusChangedEvent extends ApplicationEvent {
    private final Request request;

    public RequestStatusChangedEvent(Object source, Request request) {
        super(source);
        this.request = request;
    }
}