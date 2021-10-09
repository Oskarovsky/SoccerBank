package com.oskarro.soccerbank.config;

import com.oskarro.soccerbank.entity.ClubDataAddressUpdate;
import com.oskarro.soccerbank.entity.ClubDataBaseUpdate;
import com.oskarro.soccerbank.entity.ClubDataContactUpdate;
import com.oskarro.soccerbank.entity.ClubDataUpdate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.PatternMatchingCompositeLineTokenizer;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ImportDataJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public ImportDataJobConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job importClubDataJob() throws Exception {
        return this.jobBuilderFactory
                .get("importClubDataJob")
                .start(importClubUpdatesStep())
                .build();
    }

    @Bean
    public Step importClubUpdatesStep() throws Exception {
        return this.stepBuilderFactory
                .get("importClubUpdates")
                .<ClubDataUpdate, ClubDataUpdate>chunk(100)
                .reader(clubDataUpdateItemReader(null))
                .processor(clubDataValidatingItemProcessor(null))
                .writer(clubDataUpdateItemWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<? extends ClubDataUpdate> clubDataUpdateItemReader(@Value("#{jobParameters['clubData']}") Resource inputFile)
            throws Exception {
        return new FlatFileItemReaderBuilder<ClubDataUpdate>()
                .name("clubDataUpdateItemReader")
                .resource(inputFile)
                .lineTokenizer(clubDataUpdatesLineTokenizer())
                .fieldSetMapper(clubDataUpdateFieldSetMapper())
                .build();
    }

    @Bean
    public LineTokenizer clubDataUpdatesLineTokenizer() throws Exception {
        DelimitedLineTokenizer recordTypeOne = new DelimitedLineTokenizer();
        recordTypeOne.setNames("recordId", "clubId", "name", "yearOfFoundation");
        recordTypeOne.afterPropertiesSet();

        DelimitedLineTokenizer recordTypeTwo = new DelimitedLineTokenizer();
        recordTypeTwo.setNames("recordId", "clubId", "address1", "address2", "city", "state", "postalCode");
        recordTypeTwo.afterPropertiesSet();

        DelimitedLineTokenizer recordTypeThree = new DelimitedLineTokenizer();
        recordTypeThree.setNames("recordId", "clubId", "emailAddress", "phone", "notification");
        recordTypeThree.afterPropertiesSet();

        Map<String, LineTokenizer> tokenizerMap = new HashMap<>(3);
        tokenizerMap.put("1*", recordTypeOne);
        tokenizerMap.put("2*", recordTypeTwo);
        tokenizerMap.put("3*", recordTypeThree);

        PatternMatchingCompositeLineTokenizer tokenizer = new PatternMatchingCompositeLineTokenizer();
        tokenizer.setTokenizers(tokenizerMap);
        return tokenizer;
    }

    @Bean
    public FieldSetMapper<ClubDataUpdate> clubDataUpdateFieldSetMapper() {
        return fieldSet -> switch (fieldSet.readInt("recordId")) {
            case 1 -> new ClubDataBaseUpdate(
                    fieldSet.readLong("clubId"),
                    fieldSet.readString("name"),
                    fieldSet.readInt("yearOfFoundation"));
            case 2 -> new ClubDataAddressUpdate(
                    fieldSet.readLong("clubId"),
                    fieldSet.readString("address1"),
                    fieldSet.readString("address2"),
                    fieldSet.readString("city"),
                    fieldSet.readString("state"),
                    fieldSet.readString("postalCode"));
            case 3 -> new ClubDataContactUpdate(
                    fieldSet.readLong("clubId"),
                    fieldSet.readString("emailAddress"),
                    fieldSet.readString("phone"),
                    fieldSet.readString("notification"));
            default -> throw new IllegalArgumentException("Invalid record type was found: " + fieldSet.readInt("recordId"));
        };
    }

    @Bean
    public ItemProcessor<? super ClubDataUpdate, ? extends ClubDataUpdate> clubDataValidatingItemProcessor(
            ClubDataItemValidator validator) {
        ValidatingItemProcessor<ClubDataUpdate> clubDataUpdateValidatingItemProcessor = new ValidatingItemProcessor<>(validator);
        clubDataUpdateValidatingItemProcessor.setFilter(true);
        return clubDataUpdateValidatingItemProcessor;
    }

    @Bean
    public ItemWriter<? super ClubDataUpdate> clubDataUpdateItemWriter() {
        ClubDataUpdateClassifier classifier = new ClubDataUpdateClassifier(
                clubDataBaseUpdateJdbcBatchItemWriter(null),
                clubDataAddressUpdateJdbcBatchItemWriter(null),
                clubDataContactUpdateJdbcBatchItemWriter(null));
        ClassifierCompositeItemWriter<ClubDataUpdate> compositeItemWriter = new ClassifierCompositeItemWriter<>();
        compositeItemWriter.setClassifier(classifier);
        return compositeItemWriter;
    }

    @Bean
    public JdbcBatchItemWriter<ClubDataUpdate> clubDataBaseUpdateJdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ClubDataUpdate>()
                .beanMapped()
                .sql("UPDATE club " +
                        "SET name = COALESCE(:name, name), " +
                        "SET year_of_foundation = COALESCE(:yearOfFoundation, year_of_foundation)" +
                        "WHERE club_id = :clubId")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<ClubDataUpdate> clubDataAddressUpdateJdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ClubDataUpdate>()
                .beanMapped()
                .sql("UPDATE club " +
                        "SET address = COALESCE(:address1, address), " +
                        "WHERE club_id = :clubId")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<ClubDataUpdate> clubDataContactUpdateJdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ClubDataUpdate>()
                .beanMapped()
                .sql("UPDATE club " +
                        "SET email_address = COALESCE(:emailAddress, email_address)" +
                        "SET phone = COALESCE(:phone, phone)" +
                        "SET is_notified = COALESCE(:notification, is_notified)" +
                        "WHERE club_id = :clubId")
                .dataSource(dataSource)
                .build();
    }
}
