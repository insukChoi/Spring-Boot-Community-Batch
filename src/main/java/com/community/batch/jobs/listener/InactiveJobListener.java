package com.community.batch.jobs.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InactiveJobListener implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("Before Job");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        System.out.println("After Job");
    }
}
