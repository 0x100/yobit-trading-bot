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
class CreateSellOrderStrategy extends AbstractTradeStrategy {

    @Override
    void execute(Advice advice, Trade tradeRecord, Map currency24hInfo) {

        BigDecimal minSellProfit = getMinSellProfit(tradeRecord.amount, tradeRecord.buyPrice, botConfig.profitRate)
        log.info("need profit > ${utils.shortDecimalFormat.format(minSellProfit)}")

        BigDecimal amount = tradeRecord.amount
        BigDecimal sellPrice = advice.sellPrice - botConfig.priceAppendix

        sellPrice = adjustSellPrice(tradeRecord.pair, sellPrice)

        List<Trade> samePriceBuyOrders = tradeRepository.findByPairAndStatusAndBuyPrice(tradeRecord.pair, TradeStatus.BUY_ORDER, sellPrice)
        if(samePriceBuyOrders.empty) {

            BigDecimal buyPrice = tradeRecord.buyPrice
            BigDecimal profit = utils.calcProfit(amount, buyPrice, sellPrice)

            if (advice.stopLoss || profit > getMinSellProfit(amount, buyPrice, botConfig.profitRate)) {
                if (advice.stopLoss) {
                    log.info('SELL TO STOP A LOSS')

                    CurrencyInfo currencyInfo = currencyRepository.findOneByPair(tradeRecord.pair)
                    if (!currencyInfo.blocked) {
                        currencyInfo.blocked = true
                        currencyInfo.blockedPrice = sellPrice + sellPrice * yobitApiConfig.sellFee
                        currencyRepository.saveAndFlush(currencyInfo)
                        log.info("Currency is blocked on price ${utils.decimalFormat.format(sellPrice)}")
                    }
                }
                String currencyPair = utils.getCurrencyPair(tradeRecord.pair)
                Map createOrderResult = client.trade(currencyPair, TradeOperation.SELL, sellPrice, amount)

                if (createOrderResult.success == 1) {
                    long orderId = createOrderResult.return.order_id as Long
                    log.info("Added a sell order: id ${orderId}, price ${sellPrice}, amount ${amount}")

                    tradeRecord.sellOrderId = orderId
                    tradeRecord.sellPrice = sellPrice
                    tradeRecord.sellDate = LocalDateTime.now()
                    tradeRecord.status = TradeStatus.SELL_ORDER
                    tradeRecord.orderAttempts++

                    tradeRepository.saveAndFlush(tradeRecord)
                } else if (createOrderResult.success == 0 && createOrderResult.error == 'Insufficient funds in wallet of the first currency of the pair') {

                    BigDecimal balance = getCurrencyBalance(tradeRecord.pair)
                    if (balance > 0.0) {
                        tradeRecord.amount = balance
                    } else {
                        changeStatus(tradeRecord, TradeStatus.ERROR)
                    }
                    tradeRepository.saveAndFlush(tradeRecord)

                } else if (createOrderResult.success == 0 && createOrderResult.error.contains('Total transaction amount is less than minimal total')) {

                    changeStatus(tradeRecord, TradeStatus.ERROR)
                }
            }
        }
    }

    @Override
    boolean checkCondition(Advice advice, Trade tradeRecord, CurrencyInfo currencyInfo) {
        return advice.action == Action.SELL && tradeRecord.status == TradeStatus.PURCHASED
    }

    @Override
    Action getAdviceAction(Advice advice, Advice prevAdvice, Trade tradeRecord, Map currency24hInfo, TrendType trendType, CurrencyInfo currencyInfo) {
        Action action = Action.WAIT

        BigDecimal minSellProfit = getMinSellProfit(tradeRecord.amount, tradeRecord.buyPrice, botConfig.profitRate)
        BigDecimal profit = utils.calcProfit(tradeRecord.amount, tradeRecord.buyPrice, advice.sellPrice - botConfig.priceAppendix)

        if (tradeRecord.status == TradeStatus.PURCHASED &&
                (advice.stopLoss || profit > minSellProfit)) {

            if (prevAdvice.action == Action.WAIT_4_SELL &&
                    (prevAdvice.price && advice.price <= prevAdvice.price || profit > getMinSellProfit(tradeRecord.amount, tradeRecord.buyPrice, 0.01))) {

                action = Action.SELL

            } else {
                action = Action.WAIT_4_SELL
            }
            TradeSessionData.sellAdvicesCount++
        }
        action
    }
}
