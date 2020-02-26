package com.community.batch.jobs.listener;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.stereotype.Component;

@Component
public class InactiveStepListener {

    @BeforeStep
    public void beforeStep(StepExecution stepExecution){
        System.out.println("Before Step");
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution){
        System.out.println("After Step");
    }
}
