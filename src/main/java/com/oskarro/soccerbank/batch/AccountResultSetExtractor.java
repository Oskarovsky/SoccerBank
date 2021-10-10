package com.oskarro.soccerbank.batch;

import com.oskarro.soccerbank.entity.statement.Account;
import com.oskarro.soccerbank.entity.transaction.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccountResultSetExtractor implements ResultSetExtractor<List<Account>> {

    private List<Account> accounts = new ArrayList<>();
    private Account currentAccount;

    @Nullable
    @Override
    public List<Account> extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
            if (currentAccount == null) {
                currentAccount = new Account(
                        rs.getLong("account_id"),
                        rs.getBigDecimal("balance"),
                        rs.getDate("last_statement_timestamp"));
            } else if (rs.getLong("account_id") != currentAccount.getId()) {
                accounts.add(currentAccount);
                currentAccount = new Account(
                        rs.getLong("account_id"),
                        rs.getBigDecimal("balance"),
                        rs.getDate("last_statement_timestamp"));
            }

            if(StringUtils.hasText(rs.getString("description"))) {
                currentAccount.getTransactions().add(new Transaction(
                        rs.getLong("transaction_id"),
                        rs.getLong("account_id"),
                        rs.getString("description"),
                        rs.getBigDecimal("credit"),
                        rs.getBigDecimal("debit"),
                        new Date(rs.getTimestamp("creation_timestamp").getTime())));
            }
        }
        if (currentAccount != null) {
            accounts.add(currentAccount);
        }
        return accounts;
    }
}
