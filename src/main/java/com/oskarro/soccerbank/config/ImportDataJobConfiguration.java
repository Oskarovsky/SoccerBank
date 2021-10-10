package com.oskarro.soccerbank.config;

import com.oskarro.soccerbank.batch.*;
import com.oskarro.soccerbank.entity.clubData.ClubDataAddressUpdate;
import com.oskarro.soccerbank.entity.clubData.ClubDataBaseUpdate;
import com.oskarro.soccerbank.entity.clubData.ClubDataContactUpdate;
import com.oskarro.soccerbank.entity.clubData.ClubDataUpdate;
import com.oskarro.soccerbank.entity.statement.Club;
import com.oskarro.soccerbank.entity.statement.Statement;
import com.oskarro.soccerbank.entity.transaction.Transaction;
import io.micrometer.core.instrument.util.IOUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.PatternMatchingCompositeLineTokenizer;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
                .next(importTransactionsStep())
                .next(applyClubTransactionsStep())
                .next(generateStatementsStep(null))
                .incrementer(new RunIdIncrementer())
                .build();
    }

    // region Importing Club Data
    @Bean
    public Step importClubUpdatesStep() throws Exception {
        return this.stepBuilderFactory
                .get("importClubUpdatesStep")
                .<ClubDataUpdate, ClubDataUpdate>chunk(100)
                .reader(clubDataUpdateItemReader(null))
                .processor(clubDataValidatingItemProcessor(null))
                .writer(clubDataUpdateItemWriter())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ClubDataUpdate> clubDataUpdateItemReader(
            @Value("#{jobParameters['clubData']}") Resource inputFile) throws Exception {
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
    public ItemProcessor<ClubDataUpdate, ClubDataUpdate> clubDataValidatingItemProcessor(ClubDataItemValidator validator) {
        ValidatingItemProcessor<ClubDataUpdate> clubDataUpdateValidatingItemProcessor = new ValidatingItemProcessor<>(validator);
        clubDataUpdateValidatingItemProcessor.setFilter(true);
        return clubDataUpdateValidatingItemProcessor;
    }

    @Bean
    public ItemWriter<ClubDataUpdate> clubDataUpdateItemWriter() {
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
                        "year_of_foundation = COALESCE(:yearOfFoundation, year_of_foundation) " +
                        "WHERE club_id = :clubId")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<ClubDataUpdate> clubDataAddressUpdateJdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ClubDataUpdate>()
                .beanMapped()
                .sql("UPDATE club " +
                        "SET address = COALESCE(:address1, address) " +
                        "WHERE club_id = :clubId")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<ClubDataUpdate> clubDataContactUpdateJdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ClubDataUpdate>()
                .beanMapped()
                .sql("UPDATE club " +
                        "SET email_address = COALESCE(:emailAddress, email_address), " +
                        "phone = COALESCE(:phone, phone), " +
                        "is_notified = COALESCE(:notification, is_notified) " +
                        "WHERE club_id = :clubId")
                .dataSource(dataSource)
                .build();
    }

    // endregion

    // region Executing bank transactions
    @Bean
    public Step importTransactionsStep() {
        return this.stepBuilderFactory
                .get("importTransactionsStep")
                .<Transaction, Transaction>chunk(100)
                .reader(transactionItemReader(null))
                .writer(transactionItemWriter(null))
                .build();
    }

    @Bean
    @StepScope
    public StaxEventItemReader<Transaction> transactionItemReader(
            @Value("#{jobParameters['transactionFile']}") Resource transactionFile) {
        Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setClassesToBeBound(Transaction.class);
        return new StaxEventItemReaderBuilder<Transaction>()
                .name("transactionItemReader")
                .resource(transactionFile)
                .addFragmentRootElements("transaction")
                .unmarshaller(unmarshaller)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaction> transactionItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction>()
                .dataSource(dataSource)
                .sql("INSERT INTO transaction(transaction_id, account_id, description, credit, debit, creation_timestamp) " +
                        "VALUES (:transactionId, :accountId, :description, :credit, :debit, :creationTimestamp)")
                .beanMapped()
                .build();
    }

    // endregion

    // region Applying club transactions

    @Bean
    public Step applyClubTransactionsStep() {
        return stepBuilderFactory
                .get("applyClubTransactionsStep")
                .<Transaction, Transaction>chunk(100)
                .reader(applyClubTransactionReader(null))
                .writer(applyClubTransactionWriter(null))
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Transaction> applyClubTransactionReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Transaction>()
                .name("applyClubTransactionReader")
                .dataSource(dataSource)
                .sql("SELECT transaction_id, account_id, description, credit, debit, creation_timestamp " +
                        "FROM transaction ORDER BY creation_timestamp")
                .rowMapper((resultSet, i) ->
                        new Transaction(
                                resultSet.getLong("transaction_id"),
                                resultSet.getLong("account_id"),
                                resultSet.getString("description"),
                                resultSet.getBigDecimal("credit"),
                                resultSet.getBigDecimal("debit"),
                                resultSet.getTimestamp("creation_timestamp")))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter applyClubTransactionWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction>()
                .dataSource(dataSource)
                .sql("UPDATE account SET balance = balance + :transactionAmount WHERE account_id = :accountId")
                .beanMapped()
                .assertUpdates(false)
                .build();
    }

    // endregion

    // region Generating statements

    @Bean
    public Step generateStatementsStep(AccountItemProcessor itemProcessor) {
        return this.stepBuilderFactory
                .get("generateStatementsStep")
                .<Statement, Statement>chunk(1)
                .reader(statementItemReader(null))
                .processor(itemProcessor)
                .writer(statementItemWriter(null))
                .build();
    }

    @Bean
    public JdbcCursorItemReader statementItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Statement>()
                .name("statementItemReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM club")
                .rowMapper((resultSet, i) -> {
                    Club club = Club.builder()
                            .id(resultSet.getLong("club_id"))
                            .name(resultSet.getString("name"))
                            .address(resultSet.getString("address"))
                            .emailAddress(resultSet.getString("email_address"))
                            .phone(resultSet.getString("phone"))
                            .notification(resultSet.getString("is_notified"))
                            .yearOfFoundation(resultSet.getInt("year_of_foundation"))
                            .build();
                    return new Statement(club);
                })
                .build();
    }

    @Bean
    public FlatFileItemWriter<Statement> singleStatementItemWriter() {
        FlatFileItemWriter<Statement> itemWriter = new FlatFileItemWriter<>();
        itemWriter.setName("singleStatementItemWriter");
        itemWriter.setHeaderCallback(new StatementHeaderCallback());
        itemWriter.setLineAggregator(new StatementLineAggregator());
        return itemWriter;
    }

    @Autowired
    ResourceLoader resourceLoader;

    @Bean
    @StepScope
    public MultiResourceItemWriter<Statement> statementItemWriter(
            @Value("#{jobParameters['outputDirectory']}") Resource outputDirectory) {
        final String ROOT_PATH = "file://home/oskarro/";
        ClassPathResource classPathResource = new ClassPathResource(ROOT_PATH + outputDirectory.getFilename());
        Resource resource = resourceLoader.getResource(classPathResource.getPath());
        return new MultiResourceItemWriterBuilder<Statement>()
                .name("statementItemWriter")
                .resource(resource)
                .itemCountLimitPerResource(1)
                .delegate(singleStatementItemWriter())
                .build();
    }

    // endregion
}
