package com.oskarro.soccerbank.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClubDataUpdate {

    protected final long clubId;

    public ClubDataUpdate(long clubId) {
        this.clubId = clubId;
    }
}
