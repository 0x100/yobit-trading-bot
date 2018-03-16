package ru._x100.yobitbot.strategy

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import ru._x100.yobitbot.client.IYobitClient
import ru._x100.yobitbot.config.BotConfig
import ru._x100.yobitbot.config.YobitApiConfig
import ru._x100.yobitbot.enums.Action
import ru._x100.yobitbot.enums.TradeStatus
import ru._x100.yobitbot.enums.TrendType
import ru._x100.yobitbot.model.Advice
import ru._x100.yobitbot.model.entity.CurrencyInfo
import ru._x100.yobitbot.model.entity.Trade
import ru._x100.yobitbot.repository.CurrencyRepository
import ru._x100.yobitbot.repository.TradeRepository
import ru._x100.yobitbot.utils.Utils

@Slf4j
abstract class AbstractTradeStrategy implements TradeStrategy {

    @Autowired
    BotConfig botConfig

    @Autowired
    TradeRepository tradeRepository

    @Autowired
    CurrencyRepository currencyRepository

    @Autowired
    IYobitClient client

    @Autowired
    YobitApiConfig yobitApiConfig

    @Autowired
    Utils utils

    void changeStatus(Trade tradeRecord, TradeStatus status) {

        switch (status) {
            case TradeStatus.ACTIVE:
                tradeRecord.orderAttempts = 0
                tradeRecord.status = status
                tradeRecord.amount = 0.0
                tradeRecord.buyPrice = 0.0
                tradeRecord.sellPrice = 0.0
                tradeRecord.buyOrderId = null
                tradeRecord.sellOrderId = null
                break
            case TradeStatus.PURCHASED:
                if (tradeRecord.status == TradeStatus.BUY_ORDER) {
                    tradeRecord.orderAttempts = 0
                }
                tradeRecord.status = status
                tradeRecord.sellPrice = 0.0
                tradeRecord.sellOrderId = null
                break
            case TradeStatus.ERROR:
                tradeRecord.status = status
        }
        tradeRepository.saveAndFlush(tradeRecord)
    }

    BigDecimal adjustSellPrice(String currencyPair, BigDecimal sellPrice) {
        List<Trade> sameSellOrders = tradeRepository.findByPairAndStatusAndSellPrice(currencyPair, TradeStatus.SELL_ORDER, sellPrice)
        if (!sameSellOrders.empty) {
            return adjustSellPrice(currencyPair, sellPrice - 0.00000001)
        }
        sellPrice
    }

    boolean priceIsOutOfRange(BigDecimal source, BigDecimal current) {
        if (current == 0.0) {
            return false
        }
        BigDecimal diffInPercent = ((current - source) / source).abs()
        diffInPercent >= botConfig.diffRate2CancelOrder && (current - source).abs() > botConfig.diffToCancel
    }

    BigDecimal getCurrencyBalance(String currencyPair) {
        BigDecimal balance = 0.0

        Map info = client.accountInfo
        Map funds = info?.return?.funds
        if (funds != null) {
            balance = funds[currencyPair.toLowerCase()] as BigDecimal
            log.info("${currencyPair} balance: ${balance}")
        }
        balance
    }

    @Override
    Action getAdviceAction(Advice advice, Advice prevAdvice, Trade tradeRecord, Map currency24hInfo, TrendType trendType, CurrencyInfo currencyInfo) {
        return Action.WAIT
    }

    protected BigDecimal getMinSellProfit(BigDecimal amount, BigDecimal buyPrice, BigDecimal profitRate) {
        BigDecimal minSellSum = 0.0
        if (amount && buyPrice) {
            BigDecimal buySum = amount * buyPrice
            minSellSum = buySum * profitRate
        }
        minSellSum
    }
}
