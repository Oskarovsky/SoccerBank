package com.oskarro.soccerbank.batch;

import com.oskarro.soccerbank.entity.statement.Account;
import com.oskarro.soccerbank.entity.statement.Club;
import com.oskarro.soccerbank.entity.statement.Statement;
import com.oskarro.soccerbank.entity.transaction.Transaction;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Date;

public class StatementLineAggregator implements LineAggregator<Statement> {

    private static final String ADDRESS_LINE = String.format("%121s\n", "Apress Banking");
    private static final String STATEMENT_DATE_LINE = String.format("Your Account Summary %78s ", "Statement Period")
            + "%tD to %tD\n\n";

    @Override
    public String aggregate(Statement statement) {
        StringBuilder output = new StringBuilder();
        formatHeader(statement, output);
        formatAccount(statement, output);
        return output.toString();
    }

    private void formatAccount(Statement statement, StringBuilder output) {
        if (!CollectionUtils.isEmpty(statement.getAccounts())) {
            for (Account account : statement.getAccounts()) {
                output.append(String.format(STATEMENT_DATE_LINE, account.getLastStatementTimestamp(), new Date()));
                BigDecimal creditAmount = new BigDecimal(0);
                BigDecimal debitAmount = new BigDecimal(0);
                for (Transaction transaction : account.getTransactions()) {
                    if (transaction.getCredit() != null) {
                        creditAmount = creditAmount.add(transaction.getCredit());
                    }
                    if (transaction.getDebit() != null) {
                        debitAmount = debitAmount.add(transaction.getDebit());
                    }
                    output.append(String.format("%tD %-50s %8.2f\n",
                            transaction.getCreationTimestamp(), transaction.getDescription(), transaction.getTransactionAmount()));
                }
                output.append(String.format("%80s %14.2f\n", "Total Debit:" , debitAmount));
                output.append(String.format("%81s %13.2f\n", "Total Credit:", creditAmount));
                output.append(String.format("%76s %18.2f\n\n", "Balance:", account.getBalance()));
            }
        }
    }

    private void formatHeader(Statement statement, StringBuilder output) {
        Club club = statement.getClub();
        String clubName = String.format("\n%s", club.getName());
        output.append(clubName).append(ADDRESS_LINE.substring(clubName.length()));
    }
}
