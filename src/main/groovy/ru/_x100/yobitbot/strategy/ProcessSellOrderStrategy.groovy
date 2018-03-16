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
class ProcessSellOrderStrategy extends AbstractTradeStrategy {

    @Autowired
    IYobitClient yobitClient

    @Override
    void execute(Advice advice, Trade tradeRecord, Map currency24hInfo) {

        Map orderInfo = client.orderInfo(tradeRecord.sellOrderId).return[tradeRecord.sellOrderId.toString()] as Map

        List<Trade> sellOrders = tradeRepository.findByPairAndStatusIn(tradeRecord.pair, Collections.singletonList(TradeStatus.SELL_ORDER))
        BigDecimal minSellPrice = sellOrders.min { it.sellPrice }.sellPrice

        boolean need2CancelOrder = priceIsOutOfRange(minSellPrice, advice.sellPrice)

        if (!need2CancelOrder) {
            String currencyPair = utils.getCurrencyPair(tradeRecord.pair)
            need2CancelOrder = (yobitClient.depth(currencyPair, null)[currencyPair].asks as List).stream()
                    .map { (it as List)[0] }
                    .filter { it < minSellPrice }
                    .count() >= 3
        }

        switch (orderInfo.status) {
            case 0:
                if (need2CancelOrder) {
                    Map cancelOrder = client.cancelOrder(tradeRecord.sellOrderId)
                    if (cancelOrder.success == 1) {
                        changeStatus(tradeRecord, TradeStatus.PURCHASED)
                    }
                }
                break
            case 1:
                BigDecimal profit = utils.calcProfit(tradeRecord.amount, tradeRecord.buyPrice, tradeRecord.sellPrice)
                tradeRecord.profit = tradeRecord.profit != null && tradeRecord.profit > 0.0 ? tradeRecord.profit + profit : profit
                tradeRecord.status = TradeStatus.SOLD
                tradeRepository.saveAndFlush(tradeRecord)
                break
            case 2:
                changeStatus(tradeRecord, TradeStatus.PURCHASED)
                break
            case 3:
                if (need2CancelOrder) {
                    Map cancelOrder = client.cancelOrder(tradeRecord.sellOrderId)
                    if (cancelOrder.success == 1) {
                        BigDecimal soldAmount = orderInfo.start_amount - orderInfo.amount
                        BigDecimal profit = utils.calcProfit(soldAmount, tradeRecord.buyPrice, tradeRecord.sellPrice)
                        tradeRecord.profit = tradeRecord.profit != null && tradeRecord.profit > 0.0 ? tradeRecord.profit + profit : profit
                        tradeRecord.amount = orderInfo.amount
                        changeStatus(tradeRecord, TradeStatus.PURCHASED)
                    }
                }
        }
    }

    @Override
    boolean checkCondition(Advice advice, Trade tradeRecord, CurrencyInfo currencyInfo) {
        return tradeRecord.status == TradeStatus.SELL_ORDER
    }
}
