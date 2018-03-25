package ru._x100.yobitbot.component

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru._x100.yobitbot.client.IYobitClient
import ru._x100.yobitbot.config.BotConfig
import ru._x100.yobitbot.config.Messages
import ru._x100.yobitbot.config.YobitApiConfig
import ru._x100.yobitbot.enums.Action
import ru._x100.yobitbot.enums.CurrencyStatus
import ru._x100.yobitbot.enums.TradeStatus
import ru._x100.yobitbot.enums.TrendType
import ru._x100.yobitbot.model.Advice
import ru._x100.yobitbot.model.TradeSessionData
import ru._x100.yobitbot.model.entity.CurrencyInfo
import ru._x100.yobitbot.model.entity.Trade
import ru._x100.yobitbot.repository.CurrencyRepository
import ru._x100.yobitbot.repository.TradeRepository
import ru._x100.yobitbot.strategy.TradeStrategy
import ru._x100.yobitbot.utils.Utils

import javax.annotation.PostConstruct
import java.time.Duration
import java.time.LocalDateTime
import java.util.stream.Collectors

@Component
@Slf4j
class TradeBot {

    @Autowired
    IYobitClient client

    @Autowired
    YobitApiConfig yobitApiConfig

    @Autowired
    BotConfig botConfig

    @Autowired
    TradeRepository tradeRepository

    @Autowired
    CurrencyRepository currencyRepository

    @Autowired
    Messages messages

    @Autowired
    List<TradeStrategy> strategies

    @Autowired
    Utils utils

    Map<Long, Advice> prevAdvice = [:]
    List<CurrencyInfo> currencies

    @PostConstruct
    void init() {
        this.currencies = loadCurrencies4Trade()
    }

    @Scheduled(fixedDelayString = '${bot.tickInterval}', initialDelay = 5000L)
    void letsTrade() {

        Map trades = client.trades(preparePairCode(), yobitApiConfig.client.tradesHistoryLimit)
        Map currency24hInfo = client.ticker(preparePairCode())
        log.info('')

        TradeSessionData.buyAdvicesCount = 0
        TradeSessionData.sellAdvicesCount = 0

        this.currencies.each { CurrencyInfo currencyInfo ->
            String pairCode = utils.getCurrencyPair(currencyInfo.pair)

            List<Map> currencyTrades = trades[pairCode] as List<Map>
            if (currencyTrades?.size() > 0) {

                blockCurrencyIfBanned(currencyInfo, currencyTrades)
                List<Trade> tradeRecords = getTradeRecords(currencyInfo)
                TrendType trendType = getTrendType(currencyTrades)

                logCurrencyHeader(currencyInfo, trendType, tradeRecords, currencyTrades, pairCode, currency24hInfo)

                tradeRecords.eachWithIndex { Trade tradeRecord, int index ->
                    logTradeRecordInfo(index, tradeRecord, currency24hInfo[pairCode] as Map)
                    trade(tradeRecord, currencyTrades, currency24hInfo[pairCode] as Map, trendType, currencyInfo)
                }
            }
        }
        TradeSessionData.sessionInitialized = true
        logTotalInfo()
    }

    private void trade(Trade tradeRecord, List<Map> trades, Map currency24hInfo, TrendType trendType, CurrencyInfo currencyInfo) {

        if (tradeRecord.orderAttempts == null) {
            tradeRecord.orderAttempts = 0
        }
        String currencyPair = tradeRecord.pair
        pauseIfActiveMoreOne(tradeRecord, currencyPair)

        if (tradeRecord.status != TradeStatus.PAUSE) {
            Advice advice = getAdvice(trades, tradeRecord, currency24hInfo, trendType, currencyInfo)

            tryUnlockIfBlocked(currencyInfo, tradeRecord, currency24hInfo, advice, trendType)
            blockIfStopLoss(currencyInfo, advice)

            TradeStrategy strategy = getStrategy(advice, tradeRecord, currencyInfo)
            strategy?.execute(advice, tradeRecord, currency24hInfo)

        } else if (tradeRecord.status == TradeStatus.PAUSE) {
            checkNeedUnpause(currencyPair, tradeRecord)
        }
    }

    private Advice getAdvice(List<Map> trades, Trade tradeRecord, Map currency24hInfo, TrendType trendType, CurrencyInfo currencyInfo) {

        Advice advice = [
                action   : Action.WAIT,
                price    : trades.first().price,
                buyPrice : currency24hInfo.buy,
                sellPrice: currency24hInfo.sell,
                prevPrice: prevAdvice[tradeRecord.id]?.price
        ]
        if (prevAdvice[tradeRecord.id] != null) {

            boolean sellPriceLessBuyPrice = advice.sellPrice - 0.00000001 <= tradeRecord.buyPrice
            boolean hasLoss = calcLosses(tradeRecord, trades) > 0.0
            boolean notSoldLongTime = tradeRecord.buyDate && Math.abs(Duration.between(LocalDateTime.now(), tradeRecord.buyDate).toDays()) > botConfig.maxDayCount2Sell
            boolean banned = botConfig.bannedCurrencies?.contains(tradeRecord.pair)

            advice.stopLoss = (sellPriceLessBuyPrice || hasLoss || notSoldLongTime || banned) && trendType in [TrendType.DOWN, TrendType.MOTIONLESS]
            advice.action = getSuitableAction(advice, tradeRecord, currency24hInfo, trendType, currencyInfo)
        }
        prevAdvice[tradeRecord.id] = advice

        logAdvice(advice)
        advice
    }

    private Action getSuitableAction(Advice advice, Trade tradeRecord, Map currency24hInfo, TrendType trendType, CurrencyInfo currencyInfo) {

        for (TradeStrategy strategy : strategies) {
            Action action = strategy.getAdviceAction(advice, prevAdvice[tradeRecord.id], tradeRecord, currency24hInfo, trendType, currencyInfo)

            if (action != Action.WAIT) {
                return action
            }
        }
        Action.WAIT
    }

    /**
     * Currency list is formed by the method
     * {@link CurrencySelector#refreshCurrencyList()}
     * (by default refreshes every hour)
     */
    private List<CurrencyInfo> loadCurrencies4Trade() {

        List<CurrencyInfo> blockedCurrencies4Sell = currencyRepository.findByStatus(CurrencyStatus.BLOCKED).stream()
                .filter { !tradeRepository.findByPairAndStatusIn(it.pair, Arrays.asList(TradeStatus.PURCHASED, TradeStatus.SELL_ORDER)).empty }
                .collect()
        log.info("Blocked currencies for sell: ${blockedCurrencies4Sell}")

        List<CurrencyInfo> currencies = currencyRepository.findByStatus(CurrencyStatus.ACTIVE)
        currencies.addAll(blockedCurrencies4Sell)
        currencies
    }

    private List<Trade> getTradeRecords(CurrencyInfo currencyInfo) {

        List<Trade> tradeRecords = tradeRepository.findByPairAndStatusIn(currencyInfo.pair,
                Arrays.asList(TradeStatus.ACTIVE, TradeStatus.BUY_ORDER, TradeStatus.PURCHASED, TradeStatus.SELL_ORDER, TradeStatus.PAUSE))

        if (tradeRecords.empty || tradeRecords.size() <= botConfig.parallelTradesCount - 1) {
            Trade tradeRecord = [
                    pair         : currencyInfo.pair,
                    status       : TradeStatus.ACTIVE,
                    orderAttempts: 0]
            tradeRecord = tradeRepository.saveAndFlush(tradeRecord)
            tradeRecords << tradeRecord
        }
        tradeRecords
    }

    private TrendType getTrendType(List<Map> trades) {

        int tradesSublistSize = (int) (yobitApiConfig.client.tradesHistoryLimit / 2)
        List lastTrades = trades.subList(0, tradesSublistSize)
        List earlyTrades = trades.subList(trades.size() - tradesSublistSize, trades.size())

        BigDecimal lastTradesAvg = avgTradesSum(lastTrades)
        BigDecimal earlyTradesAvg = avgTradesSum(earlyTrades)

        boolean up = lastTradesAvg > earlyTradesAvg &&
                lastTrades.first().price > earlyTrades.first().price

        boolean down = lastTradesAvg < earlyTradesAvg &&
                lastTrades.first().price < earlyTrades.first().price

        TrendType trendType

        if (up)
            trendType = TrendType.UP
        else if (down)
            trendType = TrendType.DOWN
        else
            trendType = TrendType.MOTIONLESS

        trendType
    }

    private void tryUnlockIfBlocked(CurrencyInfo currencyInfo, Trade tradeRecord, Map currency24hInfo, Advice advice, TrendType trendType) {

        if (currencyInfo.blocked && (botConfig.bannedCurrencies == null || !botConfig.bannedCurrencies.contains(tradeRecord.pair))) {
            log.warn("Currency is blocked from price ${utils.decimalFormat.format(currencyInfo.blockedPrice)}")

            BigDecimal avg = currency24hInfo.avg
            BigDecimal low = currency24hInfo.low
            BigDecimal threshold = avg - (avg - low) / 2.0

            if (advice.price > currencyInfo.blockedPrice || trendType == TrendType.UP && advice.price > threshold) {
                currencyInfo.status = CurrencyStatus.ACTIVE
                currencyInfo.blockedPrice = 0.0
                currencyRepository.saveAndFlush(currencyInfo)
                prevAdvice[tradeRecord.id].action = Action.WAIT_4_BUY

                log.info('Currency is unlocked')
            }
        }
    }

    private void pauseIfActiveMoreOne(Trade tradeRecord, String currencyPair) {

        if (tradeRecord.status == TradeStatus.ACTIVE) {
            List pairTrades = tradeRepository.findByPairAndStatusIn(currencyPair, [TradeStatus.ACTIVE])

            if (pairTrades.size() > 1) {
                tradeRecord.status = TradeStatus.PAUSE
                tradeRecord.amount = 0.0
                tradeRecord.buyPrice = 0.0
                tradeRecord.sellPrice = 0.0
                tradeRecord.buyOrderId = null
                tradeRecord.sellOrderId = null

                tradeRepository.saveAndFlush(tradeRecord)
            }
        }
    }

    private void blockIfStopLoss(CurrencyInfo currencyInfo, Advice advice) {
        if (!currencyInfo.blocked && advice.stopLoss) {
            currencyInfo.status = CurrencyStatus.BLOCKED
            currencyInfo.blockedPrice = advice.price
            currencyRepository.saveAndFlush(currencyInfo)
        }
    }

    private BigDecimal avgTradesSum(List<Map> trades) {
        trades.collect { it.price }.sum() / trades.size()
    }

    private BigDecimal calcLosses(Trade tradeRecord, List<Map> trades) {
        BigDecimal buySum = getBuySum(tradeRecord)
        calcLosses(tradeRecord.amount, buySum, trades)
    }

    private BigDecimal calcLosses(BigDecimal amount, BigDecimal buySum, List<Map> trades) {

        BigDecimal sellPrice = trades.first().price
        BigDecimal losses = 0.0

        if (amount > 0.0 && buySum > 0.0) {

            BigDecimal sellSum = sellPrice * amount
            sellSum -= sellSum * yobitApiConfig.fee

            buySum += buySum * yobitApiConfig.fee

            BigDecimal profit = sellSum - buySum
            BigDecimal loss = -profit

            if (loss > 0.01 && buySum * botConfig.stopLossRate <= loss) {
                losses = loss
            }
        }
        losses
    }

    private BigDecimal getBuySum(Trade tradeRecord) {
        if (tradeRecord.buyPrice && tradeRecord.amount)
            tradeRecord.buyPrice * tradeRecord.amount
        else
            0.0
    }

    private void checkNeedUnpause(String currencyPair, Trade tradeRecord) {
        List pairTrades = tradeRepository.findByPairAndStatusIn(currencyPair, [TradeStatus.ACTIVE])
        if (pairTrades.empty) {

            tradeRecord.orderAttempts = 0
            tradeRecord.status = TradeStatus.ACTIVE
            tradeRecord.amount = 0.0
            tradeRecord.buyPrice = 0.0
            tradeRecord.sellPrice = 0.0
            tradeRecord.buyOrderId = null
            tradeRecord.sellOrderId = null

            tradeRepository.saveAndFlush(tradeRecord)
        }
    }

    private TradeStrategy getStrategy(Advice advice, Trade tradeRecord, CurrencyInfo currencyInfo) {
        for (TradeStrategy strategy : strategies) {
            if (strategy.checkCondition(advice, tradeRecord, currencyInfo)) {
                return strategy
            }
        }
        null
    }

    private BigDecimal currencyAmountInTrading(List<Trade> tradeRecords) {
        tradeRecords.stream()
                .filter { it.amount != null && it.status in [TradeStatus.PURCHASED, TradeStatus.SELL_ORDER] }
                .map { it.amount }
                .collect()
                .sum() as BigDecimal
    }

    private String preparePairCode() {
        return this.currencies.stream().
                map { utils.getCurrencyPair(it.pair) }.
                collect(Collectors.joining('-'))
    }

    private void blockCurrencyIfBanned(CurrencyInfo currencyInfo, List<Map> currencyTrades) {
        if (!currencyInfo.blocked && botConfig.bannedCurrencies?.contains(currencyInfo)) {
            currencyInfo.blocked = true
            currencyInfo.blockedPrice = currencyTrades.first().price
            currencyRepository.saveAndFlush(currencyInfo)
        }
    }

    private String getCurrencyPriceInfo(List<Map> currencyTrades, Map currency24hInfo) {

        String currentPriceStr = utils.decimalFormat.format(currencyTrades.first().price)
        String highPriceStr = utils.decimalFormat.format(currency24hInfo.high)
        String avgPriceStr = utils.decimalFormat.format(currency24hInfo.avg)
        String lowPriceStr = utils.decimalFormat.format(currency24hInfo.low)
        String buyPriceStr = utils.decimalFormat.format(currency24hInfo.buy)
        String sellPriceStr = utils.decimalFormat.format(currency24hInfo.sell)

        "price ${currentPriceStr}, buy ${buyPriceStr}, sell ${sellPriceStr}"
    }

    private void logCurrencyHeader(CurrencyInfo currencyInfo, TrendType trendType, List<Trade> tradeRecords, List<Map> currencyTrades, String pairCode, Map currency24hInfo) {
        log.info '--------------------------------------------------------------------------------'
        log.info " ${currencyInfo.pair} ${getTrendArrow(trendType)}(${utils.decimalFormat.format(currencyAmountInTrading(tradeRecords) ?: 0.0)}${currencyInfo.blocked ? ', BLOCKED' : ''}) ${getCurrencyPriceInfo(currencyTrades, currency24hInfo[pairCode] as Map)}"
    }

    private void logTradeRecordInfo(int index, Trade tradeRecord, Map currency24hInfo) {
        log.debug('')
        log.info("${index + 1}. ${tradeRecord.status}")
        log.debug("${utils.getTradeRecordAsString(tradeRecord, currency24hInfo.sell as BigDecimal)}")
    }

    private void logTotalInfo() {
        log.info('')
        log.info('================================================================================')
        log.info('')
        log.info("Buy advices count: ${TradeSessionData.buyAdvicesCount}")
        log.info("Sell advices count: ${TradeSessionData.sellAdvicesCount}")
        log.info('')

        log.info("Total profit: ${utils.shortDecimalFormat.format(tradeRepository.getTotalProfit(TradeStatus.SOLD))} ${botConfig.baseCurrency}")
        log.info("Now in trading: ${utils.shortDecimalFormat.format(tradeRepository.getTradingSum(Arrays.asList(TradeStatus.PURCHASED, TradeStatus.SELL_ORDER), this.currencies.stream().map { it.pair }.collect(Collectors.toList())) ?: 0.0)} ${botConfig.baseCurrency}")
        log.info('')
    }

    private String getTrendArrow(TrendType trendType) {
        String trendArrow = ''
        switch (trendType) {
            case TrendType.UP:
                trendArrow = '↑ UP '
                break
            case TrendType.DOWN:
                trendArrow = '↓ DOWN '
        }
        trendArrow
    }

    private void logAdvice(Advice advice) {
        String actionName = advice.action.name().toLowerCase()
        String adviceAction = messages.action[actionName]
        log.info("Advice: ${adviceAction.toUpperCase()}")
    }
}
