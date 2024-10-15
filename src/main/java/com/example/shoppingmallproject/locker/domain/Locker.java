package com.example.shoppingmallproject.locker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Locker {

    @Id @GeneratedValue
    private Long lockerId;

    private int lockerNumber;
    private Boolean lockerStatus;
}
