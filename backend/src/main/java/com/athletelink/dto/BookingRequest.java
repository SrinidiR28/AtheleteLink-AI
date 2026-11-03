package com.athletelink.dto;

import jakarta.validation.constraints.NotNull;

public class BookingRequest {
    @NotNull private Long slotId;
    public Long getSlotId(){return slotId;} public void setSlotId(Long v){slotId=v;}
}
