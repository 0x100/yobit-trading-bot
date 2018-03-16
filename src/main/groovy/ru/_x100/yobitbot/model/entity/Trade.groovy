package ru._x100.yobitbot.model.entity

import ru._x100.yobitbot.enums.TradeStatus

import javax.persistence.*
import java.time.LocalDateTime

@Entity
class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id
    String pair
    BigDecimal amount
    Long buyOrderId
    BigDecimal buyPrice
    LocalDateTime buyDate
    Long sellOrderId
    BigDecimal sellPrice
    LocalDateTime sellDate
    BigDecimal profit
    Integer orderAttempts

    @Enumerated(EnumType.STRING)
    TradeStatus status

    LocalDateTime modifTime

    @PrePersist
    @PreUpdate
    void updateModifTime() {
        modifTime = LocalDateTime.now()
    }
}
