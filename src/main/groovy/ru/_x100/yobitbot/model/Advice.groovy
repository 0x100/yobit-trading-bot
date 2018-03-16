package ru._x100.yobitbot.model

import ru._x100.yobitbot.enums.Action

class Advice {

    BigDecimal price
    BigDecimal buyPrice
    BigDecimal sellPrice
    BigDecimal prevPrice
    Action action
    boolean stopLoss
}
