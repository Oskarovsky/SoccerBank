package com.oskarro.soccerbank.entity.statement;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Club {

    private final long id;
    private final String name;
    private final String address;
    private final String emailAddress;
    private final String phone;
    private final String notification;
    private final int yearOfFoundation;

    public Club(long id, String name, String address, String emailAddress, String phone, String notification, int yearOfFoundation) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.emailAddress = emailAddress;
        this.phone = phone;
        this.notification = notification;
        this.yearOfFoundation = yearOfFoundation;
    }
}
