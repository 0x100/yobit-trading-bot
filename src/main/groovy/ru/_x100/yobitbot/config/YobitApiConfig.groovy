package ru._x100.yobitbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = 'yobit')
class YobitApiConfig {

    String publicApiUrl
    String tradingApiUrl
    String key
    String secretKey
    BigDecimal buyFee
    BigDecimal sellFee
    Client client
}
