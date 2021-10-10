package com.oskarro.soccerbank.entity.clubData;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClubDataBaseUpdate extends ClubDataUpdate {

    private final String name;
    private final int yearOfFoundation;

    public ClubDataBaseUpdate(long clubId, String name, int yearOfFoundation) {
        super(clubId);
        this.name = name;
        this.yearOfFoundation = yearOfFoundation;
    }
}
