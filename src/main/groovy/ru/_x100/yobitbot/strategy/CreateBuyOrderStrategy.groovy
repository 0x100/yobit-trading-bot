package ru._x100.yobitbot.strategy

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import ru._x100.yobitbot.enums.Action
import ru._x100.yobitbot.enums.TradeOperation
import ru._x100.yobitbot.enums.TradeStatus
import ru._x100.yobitbot.enums.TrendType
import ru._x100.yobitbot.model.Advice
import ru._x100.yobitbot.model.TradeSessionData
import ru._x100.yobitbot.model.entity.CurrencyInfo
import ru._x100.yobitbot.model.entity.Trade

import java.time.LocalDateTime

@Component
@Slf4j
class CreateBuyOrderStrategy extends AbstractTradeStrategy {

    @Override
    void execute(Advice advice, Trade tradeRecord, Map currency24hInfo) {

        List pairTrades = tradeRepository.findByPairAndStatusIn(tradeRecord.pair, [TradeStatus.BUY_ORDER])
        if (pairTrades.empty) {

            BigDecimal maxBuyPrice = getBuyThreshold(currency24hInfo)
            log.info("${advice.price - maxBuyPrice < 0.0 ? 'good' : 'bad'} price for buy, " + "need < ${utils.decimalFormat.format(maxBuyPrice)}")

            BigDecimal baseCurrencyBalance = getCurrencyBalance(botConfig.baseCurrency)
            BigDecimal balance = baseCurrencyBalance - botConfig.frozenBaseCurrencyBalance
            if (balance > 0.0) {

                BigDecimal sum = balance / activeCurrenciesCount
                if (sum > 0.0) {

                    if (sum > balance * botConfig.baseCurrencyBalanceRateCanUse) {
                        sum = balance * botConfig.baseCurrencyBalanceRateCanUse
                    }
                    if (sum > botConfig.maxBaseCurrencyBalanceCanUse) {
                        sum = botConfig.maxBaseCurrencyBalanceCanUse
                    }
                    if (sum >= botConfig.minBuySum) {

                        BigDecimal buyPrice = calcBuyPrice(advice)

                        if (buyPrice < getBuyThreshold(currency24hInfo)) {

                            List<Trade> samePriceSellOrders = tradeRepository.findByPairAndStatusAndSellPrice(tradeRecord.pair, TradeStatus.SELL_ORDER, buyPrice)
                            if(samePriceSellOrders.empty) {

                                BigDecimal amount = sum / buyPrice

                                Map createOrderResult = createBuyOrder(tradeRecord, buyPrice, amount)
                                if (createOrderResult.success == 1) {
                                    saveSuccessfulBuyOrderInfo(createOrderResult, buyPrice, amount, tradeRecord)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int getActiveCurrenciesCount() {
        TradeSessionData.sessionInitialized ? TradeSessionData.buyAdvicesCount : 0
    }

    private void saveSuccessfulBuyOrderInfo(Map createOrderResult, BigDecimal buyPrice, BigDecimal amount, Trade tradeRecord) {
        long orderId = createOrderResult.return.order_id as Long
        log.info("Added a buy order: id ${orderId}, price ${buyPrice}, amount ${amount}")

        tradeRecord.buyOrderId = orderId
        tradeRecord.amount = amount
        tradeRecord.buyPrice = buyPrice
        tradeRecord.buyDate = LocalDateTime.now()
        tradeRecord.status = TradeStatus.BUY_ORDER
        tradeRecord.orderAttempts++
        tradeRepository.saveAndFlush(tradeRecord)
    }

    private Map createBuyOrder(Trade tradeRecord, BigDecimal buyPrice, BigDecimal amount) {
        String currencyPair = utils.getCurrencyPair(tradeRecord.pair)
        Map createOrderResult = client.trade(currencyPair, TradeOperation.BUY, buyPrice, amount)
        return createOrderResult
    }

    private BigDecimal calcBuyPrice(Advice advice) {
        advice.buyPrice + botConfig.priceAppendix
    }

    @Override
    boolean checkCondition(Advice advice, Trade tradeRecord, CurrencyInfo currencyInfo) {
        return advice.action == Action.BUY && tradeRecord.status == TradeStatus.ACTIVE && !currencyInfo.blocked
    }

    @Override
    Action getAdviceAction(Advice advice, Advice prevAdvice, Trade tradeRecord, Map currency24hInfo, TrendType trendType, CurrencyInfo currencyInfo) {
        Action action = Action.WAIT

        BigDecimal buyThreshold = getBuyThreshold(currency24hInfo)
        BigDecimal minSellProfit = getMinSellProfit(1.0, advice.buyPrice + botConfig.priceAppendix, 0.01)

        if (tradeRecord.status == TradeStatus.ACTIVE && !currencyInfo.blocked && advice.buyPrice + botConfig.priceAppendix < buyThreshold &&
                utils.calcProfit(1.0, advice.buyPrice + botConfig.priceAppendix, advice.sellPrice - botConfig.priceAppendix) > minSellProfit) {

            if (prevAdvice.action == Action.WAIT_4_BUY &&
                    prevAdvice.price &&
                    advice.price >= prevAdvice.price || prevAdvice.action == Action.BUY) {

                action = Action.BUY

            } else {
                action = Action.WAIT_4_BUY
            }
            TradeSessionData.buyAdvicesCount++
        }
        action
    }

    private BigDecimal getBuyThreshold(Map currency24hInfo) {

        BigDecimal high = currency24hInfo.high
        BigDecimal avg = currency24hInfo.avg
        BigDecimal low = currency24hInfo.low

        BigDecimal threshold = 0.0

        if (avg > 0.0 && high > 0.0 && low > 0.0) {
            threshold = avg + (high - avg) * 0.75
            BigDecimal priceLimit = low * BigDecimal.valueOf(botConfig.lowestPriceMaxExceedTimes)
            return threshold <= priceLimit ? threshold : priceLimit
        }
        threshold
    }
}
