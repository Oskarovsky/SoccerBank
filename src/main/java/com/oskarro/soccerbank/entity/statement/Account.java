package com.oskarro.soccerbank.entity.statement;

import com.oskarro.soccerbank.entity.transaction.Transaction;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class Account {

    private final long id;
    private final BigDecimal balance;
    private final Date lastStatementTimestamp;
    private final List<Transaction> transactions = new ArrayList<>();

    public Account(long id, BigDecimal balance, Date lastStatementTimestamp) {
        this.id = id;
        this.balance = balance;
        this.lastStatementTimestamp = lastStatementTimestamp;
    }
}
