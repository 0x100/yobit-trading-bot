package ru._x100.yobitbot.strategy

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru._x100.yobitbot.client.IYobitClient
import ru._x100.yobitbot.enums.TradeStatus
import ru._x100.yobitbot.model.Advice
import ru._x100.yobitbot.model.entity.CurrencyInfo
import ru._x100.yobitbot.model.entity.Trade

@Component
@Slf4j
class ProcessBuyOrderStrategy extends AbstractTradeStrategy {

    @Autowired
    IYobitClient yobitClient

    @Override
    void execute(Advice advice, Trade tradeRecord, Map currency24hInfo) {

        Map orderInfo = client.orderInfo(tradeRecord.buyOrderId).return[tradeRecord.buyOrderId.toString()] as Map

        List<Trade> buyOrders = tradeRepository.findByPairAndStatusIn(tradeRecord.pair, Collections.singletonList(TradeStatus.BUY_ORDER))
        BigDecimal maxBuyPrice = buyOrders.max { it.buyPrice }.buyPrice

        boolean need2CancelOrder = priceIsOutOfRange(maxBuyPrice, advice.buyPrice) || advice.stopLoss || currencyRepository.findOneByPair(tradeRecord.pair).blocked

        if (!need2CancelOrder) {
            String currencyPair = utils.getCurrencyPair(tradeRecord.pair)
            need2CancelOrder = (yobitClient.depth(currencyPair, null)[currencyPair].bids as List).stream()
                    .map { (it as List)[0] }
                    .filter { it > maxBuyPrice }
                    .count() >= 3
        }

        if(!need2CancelOrder) {
            BigDecimal minSellProfit = getMinSellProfit(tradeRecord.amount, tradeRecord.buyPrice, 0.01)
            need2CancelOrder = utils.calcProfit(tradeRecord.amount, tradeRecord.buyPrice, advice.sellPrice - botConfig.priceAppendix) < minSellProfit
        }
        switch (orderInfo.status) {
            case 0:
                if (need2CancelOrder) {
                    Map cancelOrder = client.cancelOrder(tradeRecord.buyOrderId)
                    if (cancelOrder.success == 1) {
                        changeStatus(tradeRecord, TradeStatus.ACTIVE)
                    }
                }
                break
            case 1:
                changeStatus(tradeRecord, TradeStatus.PURCHASED)
                break
            case 2:
                changeStatus(tradeRecord, TradeStatus.ACTIVE)
                break
            case 3:
                if (need2CancelOrder) {
                    Map cancelOrder = client.cancelOrder(tradeRecord.buyOrderId)
                    if (cancelOrder.success == 1) {
                        BigDecimal purchasedAmount = orderInfo.start_amount - orderInfo.amount
                        tradeRecord.amount = purchasedAmount
                        changeStatus(tradeRecord, TradeStatus.PURCHASED)
                    }
                }
        }
    }

    @Override
    boolean checkCondition(Advice advice, Trade tradeRecord, CurrencyInfo currencyInfo) {
        return tradeRecord.status == TradeStatus.BUY_ORDER
    }
}
