package com.oskarro.soccerbank.entity.clubData;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class ClubDataAddressUpdate extends ClubDataUpdate {

    private final String address1;
    private final String address2;
    private final String city;
    private final String state;
    private final String postalCode;

    public ClubDataAddressUpdate(final long clubId, final String address1, final String address2,
                                 final String city, final String state, final String postalCode) {
        super(clubId);
        this.address1 = StringUtils.hasText(address1) ? address1 : null;
        this.address2 = StringUtils.hasText(address2) ? address2 : null;
        this.city = StringUtils.hasText(city) ? city : null;
        this.state = StringUtils.hasText(state) ? state : null;
        this.postalCode = StringUtils.hasText(postalCode) ? postalCode : null;
    }
}
