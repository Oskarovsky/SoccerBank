package com.oskarro.soccerbank.config;

import com.oskarro.soccerbank.entity.ClubDataAddressUpdate;
import com.oskarro.soccerbank.entity.ClubDataBaseUpdate;
import com.oskarro.soccerbank.entity.ClubDataContactUpdate;
import com.oskarro.soccerbank.entity.ClubDataUpdate;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.classify.Classifier;

public class ClubDataUpdateClassifier implements Classifier<ClubDataUpdate, ItemWriter<? super ClubDataUpdate>> {

    private final JdbcBatchItemWriter<ClubDataUpdate> recordTypeOneItemWriter;
    private final JdbcBatchItemWriter<ClubDataUpdate> recordTypeTwoItemWriter;
    private final JdbcBatchItemWriter<ClubDataUpdate> recordTypeThreeItemWriter;

    public ClubDataUpdateClassifier(JdbcBatchItemWriter<ClubDataUpdate> recordTypeOneItemWriter,
                                    JdbcBatchItemWriter<ClubDataUpdate> recordTypeTwoItemWriter,
                                    JdbcBatchItemWriter<ClubDataUpdate> recordTypeThreeItemWriter) {
        this.recordTypeOneItemWriter = recordTypeOneItemWriter;
        this.recordTypeTwoItemWriter = recordTypeTwoItemWriter;
        this.recordTypeThreeItemWriter = recordTypeThreeItemWriter;
    }

    @Override
    public ItemWriter<? super ClubDataUpdate> classify(ClubDataUpdate classifiable) {
        if (classifiable instanceof ClubDataBaseUpdate) {
            return recordTypeOneItemWriter;
        } else if (classifiable instanceof ClubDataAddressUpdate) {
            return recordTypeTwoItemWriter;
        } else if (classifiable instanceof ClubDataContactUpdate) {
            return recordTypeThreeItemWriter;
        } else {
            throw new IllegalArgumentException(String.format("Invalid type: %s", classifiable.getClass().getCanonicalName()));
        }
    }
}
