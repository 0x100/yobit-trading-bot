package ru._x100.yobitbot.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru._x100.yobitbot.config.BotConfig
import ru._x100.yobitbot.config.YobitApiConfig
import ru._x100.yobitbot.model.entity.Trade

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.text.DecimalFormat

@Component
class Utils {

    public static final DecimalFormat decimalFormat = new DecimalFormat('0.00000000')
    public static final DecimalFormat shortDecimalFormat = new DecimalFormat('0.00')

    private static final String HMAC_SHA512 = 'HmacSHA512'
    private static final String ENCODING = 'UTF-8'

    @Autowired
    YobitApiConfig yobitApiConfig

    @Autowired
    BotConfig botConfig

    String signParams(String secretKey, String params) {
        Mac hmacSha512 = Mac.getInstance(HMAC_SHA512)
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(ENCODING), HMAC_SHA512)
        hmacSha512.init(keySpec)
        byte[] signature = hmacSha512.doFinal(params.getBytes(ENCODING))
        bytesToHex(signature)
    }

    BigDecimal calcProfit(BigDecimal amount, BigDecimal buyPrice, BigDecimal sellPrice) {
        BigDecimal profit = 0.0
        if (sellPrice && amount && buyPrice) {
            BigDecimal sellSum = sellPrice * amount
            profit = sellSum - sellSum * yobitApiConfig.sellFee - buyPrice * amount * yobitApiConfig.buyFee
        }
        profit
    }

    String getTradeRecordAsString(Trade tradeRecord, BigDecimal currentSellPrice) {
        String result = "${tradeRecord.status} "

        if (tradeRecord.amount) {
            result += ": ${decimalFormat.format(tradeRecord.amount)}"

            if (tradeRecord.buyPrice) {
                result += ", sum: ${shortDecimalFormat.format(tradeRecord.buyPrice * tradeRecord.amount)}"
                result += ", price: ${decimalFormat.format(tradeRecord.buyPrice)}"

                BigDecimal sellPrice = (tradeRecord.sellPrice ?: currentSellPrice) - botConfig.priceAppendix
                result += ", profit: ${decimalFormat.format(calcProfit(tradeRecord.amount, tradeRecord.buyPrice, sellPrice))} (${shortDecimalFormat.format(((sellPrice - tradeRecord.buyPrice) / tradeRecord.buyPrice) * 100.0)}%)"

                if (tradeRecord.sellPrice) {
                    result += ", sell price: ${decimalFormat.format(tradeRecord.sellPrice)}"
                }
            }
        }
        return result
    }

    String getCurrencyPair(String currencyCode) {
        "${currencyCode}_${botConfig.baseCurrency}".toLowerCase()
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = '0123456789abcdef'.toCharArray()
        char[] hexChars = new char[bytes.length * 2]
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF
            hexChars[j * 2] = hexArray[v >>> 4]
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]
        }
        new String(hexChars)
    }
}
