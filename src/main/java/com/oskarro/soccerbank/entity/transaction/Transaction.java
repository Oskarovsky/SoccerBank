package com.oskarro.soccerbank.entity.transaction;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@XmlRootElement(name = "transaction")
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private long transactionId;
    private long accountId;
    private String description;
    private BigDecimal credit;
    private BigDecimal debit;
    private Date timestamp;

    @XmlJavaTypeAdapter(JaxbDateSerializer.class)
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getTransactionAmount() {
        if (credit != null) {
            if (debit != null) {
                return credit.add(debit);
            }
            else {
                return credit;
            }
        } else return Objects.requireNonNullElseGet(debit, () -> new BigDecimal(0));
    }
}
