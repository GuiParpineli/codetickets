package com.quarkbyte.codetickets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.time.LocalDateTime;

@Configuration
public class ImportationJobConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ImportationJobConfiguration.class);
    private final PlatformTransactionManager transactionManager;

    public ImportationJobConfiguration(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }


    @Bean
    public Job job(Step initialStep, JobRepository jobRepository) {
        return new JobBuilder("geracao-tickets", jobRepository)
                .start(initialStep)
                .next(moveProccesedFilesStep(jobRepository))
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step initialStep(ItemReader<Importation> reader, ItemWriter<Importation> writer, JobRepository jobRepository) {
        return new StepBuilder("initial-step", jobRepository)
                .<Importation, Importation>chunk(200, transactionManager)
                .reader(reader)
                .processor(processor())
                .writer(writer)
                .build();
    }

    @Bean
    public ItemReader<Importation> reader() {
        return new FlatFileItemReaderBuilder<Importation>()
                .name("leitura-csv")
                .resource(new FileSystemResource("files/dados.csv"))
                .comments("--")
                .delimited()
                .delimiter(";")
                .names("cpf", "cliente", "nascimento", "evento", "data", "tipoIngresso", "valor")
                .fieldSetMapper(new ImportationMapper())
                .build();
    }

    @Bean
    public ItemWriter<Importation> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Importation>()
                .dataSource(dataSource)
                .sql(
                        """
                                INSERT INTO importation (cpf, cliente, nascimento, evento, data, tipo_ingresso, valor, hora_importacao, taxa_adm) VALUES
                                (:cpf, :cliente, :nascimento, :evento, :data, :tipoIngresso, :valor,:horaImportacao, :taxaAdm)"""
                )
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
    }

    @Bean
    public ImportationProcessor processor() {
        return new ImportationProcessor();
    }

    @Bean
    public Tasklet moveFilesTasklet() {
        return (contribution, chunkContext) -> {
            File origin = new File("files");
            File destin = new File("files/processed");
            if (!destin.exists()) {
                destin.mkdirs();
            }

            File[] files = origin.listFiles((dir, name) -> name.endsWith(".csv"));
            if (files != null) {
                for (File file : files) {
                    File dest = new File(destin, file.getName());
                    if (file.renameTo(dest))
                        log.info("Arquivo movido: {}", file.getAbsolutePath());
                    else throw new RuntimeException("Erro ao mover aquivo: " + file.getAbsolutePath());
                }
            }
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step moveProccesedFilesStep(JobRepository jobRepository) {
        return new StepBuilder("mover-arquivo", jobRepository)
                .tasklet(moveFilesTasklet(), transactionManager)
                .build();
    }
}
