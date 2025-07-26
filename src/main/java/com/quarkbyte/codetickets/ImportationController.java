package com.quarkbyte.codetickets;

import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImportationController {
    private final JobLauncher jobLauncher;
    private final Job job;
    private final JobExplorer jobExplorer;

    public ImportationController(JobLauncher jobLauncher, Job job, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.jobExplorer = jobExplorer;
    }

    @GetMapping("/import")
    public ResponseEntity<String> importFiles() {
        try {
            if (isJobCurrentlyRunning()) {
                return ResponseEntity.badRequest()
                        .body("Job de importação já está sendo executado. Aguarde a conclusão.");
            }

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            return ResponseEntity.ok("Job de importação iniciado com ID: " + jobExecution.getId());

        } catch (JobExecutionAlreadyRunningException e) {
            return ResponseEntity.badRequest()
                    .body("Job já está sendo executado: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erro ao iniciar job: " + e.getMessage());
        }
    }

    private boolean isJobCurrentlyRunning() {
        return jobExplorer.findRunningJobExecutions("geracao-tickets")
                .stream()
                .anyMatch(execution -> execution.getStatus() == BatchStatus.STARTED);
    }
}