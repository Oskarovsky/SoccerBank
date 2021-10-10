package com.oskarro.soccerbank.batch;

import com.oskarro.soccerbank.entity.statement.Account;
import com.oskarro.soccerbank.entity.statement.Statement;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountItemProcessor implements ItemProcessor<Statement, Statement> {

    private final JdbcTemplate jdbcTemplate;

    public AccountItemProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Statement process(Statement statement) throws Exception {
        statement.setAccounts(this.jdbcTemplate
                .query(
                        "SELECT a.account_id, a.balance, a.last_statement_timestamp, " +
                                "t.transaction_id, t.description, t.credit, t.debit, t.creation_timestamp " +
                                "FROM account a " +
                                "LEFT JOIN transaction t ON a.account_id = t.account_id " +
                                "WHERE a.account_id IN " +
                                "(SELECT account_account_id FROM club_account WHERE club_club_id = ? " +
                                "ORDER BY t.creation_timestamp)",
                new Object[] {statement.getClub().getId()}, new AccountResultSetExtractor()));
        return statement;
    }
}
