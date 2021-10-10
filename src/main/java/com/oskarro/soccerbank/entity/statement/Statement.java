package com.oskarro.soccerbank.entity.statement;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Statement {

    private Club club;
    private List<Account> accounts = new ArrayList<>();

    public Statement(Club club, List<Account> accounts) {
        this.club = club;
        this.accounts.addAll(accounts);
    }

    public Statement(Club club) {
        this.club = club;
    }
}
