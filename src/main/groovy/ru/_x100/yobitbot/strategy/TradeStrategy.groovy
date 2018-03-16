package ru._x100.yobitbot.strategy

import ru._x100.yobitbot.enums.Action
import ru._x100.yobitbot.enums.TrendType
import ru._x100.yobitbot.model.Advice
import ru._x100.yobitbot.model.entity.CurrencyInfo
import ru._x100.yobitbot.model.entity.Trade

interface TradeStrategy {

    void execute(Advice advice, Trade tradeRecord, Map currency24hInfo)

    boolean checkCondition(Advice advice, Trade tradeRecord, CurrencyInfo currencyInfo)

    Action getAdviceAction(Advice advice, Advice prevAdvice, Trade tradeRecord, Map currency24hInfo, TrendType trendType, CurrencyInfo currencyInfo)
}
