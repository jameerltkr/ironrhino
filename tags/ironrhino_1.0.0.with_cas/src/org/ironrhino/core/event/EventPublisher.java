package org.ironrhino.core.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component("eventPublisher")
public class EventPublisher {

	@Autowired
	private ApplicationEventPublisher publisher;

	public void publish(ApplicationEvent event) {
		this.publisher.publishEvent(event);
	}
}