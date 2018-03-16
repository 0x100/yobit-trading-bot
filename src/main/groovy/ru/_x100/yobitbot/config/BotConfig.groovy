package ru._x100.yobitbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = 'bot')
class BotConfig {

    String baseCurrency
    List<String> currencies
    List<String> bannedCurrencies
    int parallelTradesCount
    int maxDayCount2Sell
    int maxOrderAttempts
    BigDecimal lowestPriceMaxExceedTimes
    BigDecimal rurBalanceRateCanUse
    BigDecimal maxRurBalanceCanUse
    BigDecimal profitRate
    BigDecimal stopLossRate
    BigDecimal diffRate2CancelOrder
    BigDecimal frozenRurBalance
    BigDecimal minBuySum
    BigDecimal priceAppendix
    BigDecimal diffToCancel
    BigDecimal currencyFilterThreshold
    BigDecimal currencyVolumeThreshold
}
