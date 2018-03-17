package ru._x100.yobitbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = 'bot')
class BotConfig {

    String baseCurrency
    BigDecimal baseCurrencyBalanceRateCanUse
    BigDecimal maxBaseCurrencyBalanceCanUse
    BigDecimal frozenBaseCurrencyBalance
    List<String> currencies
    List<String> bannedCurrencies
    int parallelTradesCount
    int maxDayCount2Sell
    BigDecimal lowestPriceMaxExceedTimes
    BigDecimal profitRate
    BigDecimal stopLossRate
    BigDecimal diffRate2CancelOrder
    BigDecimal minBuySum
    BigDecimal priceAppendix
    BigDecimal diffToCancel
    BigDecimal currencyFilterThreshold
    BigDecimal currencyVolumeThreshold
}
