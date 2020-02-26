package com.community.batch.jobs;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

import java.util.Random;

public class InactiveJobExecutionDecider implements JobExecutionDecider {
    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        if (new Random().nextInt() > 0){
            System.out.println("FlowExecutionStatus.COMPLETED");
            return FlowExecutionStatus.COMPLETED;
        }
        System.out.println("FlowExecutionStatus.FAILED");
        return FlowExecutionStatus.FAILED;
    }
}
