package com.oskarro.soccerbank.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class ClubDataContactUpdate extends ClubDataUpdate {

    private final String emailAddress;
    private final String phone;
    private final String notification;

    public ClubDataContactUpdate(final long clubId, String emailAddress, String phone, String notification) {
        super(clubId);
        this.emailAddress = StringUtils.hasText(emailAddress) ? emailAddress : null;
        this.phone = StringUtils.hasText(phone) ? phone : null;
        this.notification = StringUtils.hasText(notification) ? notification : null;
    }
}
