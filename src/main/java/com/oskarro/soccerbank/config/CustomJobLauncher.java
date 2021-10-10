package com.oskarro.soccerbank.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomJobLauncher {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    Job importClubDataJob;

    public void run() {

        try {
            new JobParametersBuilder().addLong("time",System.currentTimeMillis()).toJobParameters();
            JobExecution execution = jobLauncher.run(importClubDataJob, new JobParameters());
            System.out.println("Exit Status : " + execution.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
