package ru._x100.yobitbot.model.entity

import groovy.transform.ToString
import ru._x100.yobitbot.enums.CurrencyStatus

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.PrePersist
import javax.persistence.PreUpdate
import java.time.LocalDateTime

@Entity
@ToString
class CurrencyInfo {

    CurrencyInfo() {
    }

    CurrencyInfo(String pair) {
        this.pair = pair
    }

    @Id
    String pair

    @Enumerated(EnumType.STRING)
    CurrencyStatus status

    BigDecimal blockedPrice
    LocalDateTime modifTime

    @PrePersist
    @PreUpdate
    void updateModifTime() {
        modifTime = LocalDateTime.now()
    }

    boolean isBlocked() {
        status == CurrencyStatus.BLOCKED
    }
}
