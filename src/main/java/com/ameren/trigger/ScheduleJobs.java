package com.ameren.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling

public class ScheduleJobs {

	@Autowired
	private Job worker;
	
	@Scheduled(fixedDelay = 15000)
	public void autoTriggerJob() {
		if(worker.autoRun()) {
			System.out.println("run on schedule");
		}
	}
	
}