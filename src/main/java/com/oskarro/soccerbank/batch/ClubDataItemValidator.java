package com.oskarro.soccerbank.batch;

import com.oskarro.soccerbank.entity.clubData.ClubDataUpdate;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

@Component
public class ClubDataItemValidator implements Validator<ClubDataUpdate> {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String FIND_CLUB_BY_ID = "SELECT count(*) FROM club WHERE club_id = :id";

    public ClubDataItemValidator(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public void validate(ClubDataUpdate clubDataUpdate) throws ValidationException {
        Map<String, Long> map = Collections.singletonMap("id", clubDataUpdate.getClubId());
        Long count = jdbcTemplate.queryForObject(FIND_CLUB_BY_ID, map, Long.class);
        if (count == 0) {
            throw new ValidationException(String.format("Club with id %s has not found in DB", clubDataUpdate.getClubId()));
        }
    }
}
