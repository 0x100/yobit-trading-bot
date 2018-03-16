package ru._x100.yobitbot.client.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import ru._x100.yobitbot.client.IYobitClient
import ru._x100.yobitbot.config.YobitApiConfig
import ru._x100.yobitbot.enums.TradeOperation
import ru._x100.yobitbot.repository.YobitRepository
import ru._x100.yobitbot.utils.Utils

@Component
@Slf4j
class YobitClient implements IYobitClient {

    private static final String SIGNATURE_HEADER = 'Sign'

    @Autowired
    YobitApiConfig clientProperties

    @Autowired
    RestTemplate httpClient

    @Autowired
    HttpHeaders headers

    @Autowired
    YobitRepository yobitRepository

    @Autowired
    Utils utils

    @Autowired
    YobitApiConfig yobitApiConfig

    // --------------------------------------------------------------------

    @Override
    Map info() {
        log.debug('')
        log.debug("calling info...")

        def data = callPublicApi('info', null, null)

        log.debug("data: ${data}")
        data
    }

    @Override
    Map ticker(String pair) {
        log.debug('')
        log.debug("calling ticker for pair ${pair}...")

        def data = callPublicApi('ticker', pair, null)

        log.debug("data: ${data}")
        data
    }

    @Override
    Map depth(String pair, Integer limit) {
        log.debug('')
        log.debug("calling depth for pair ${pair}...")

        def data = callPublicApi('depth', pair, limit ?: yobitApiConfig.client.depthLimit)

        log.debug("data: ${data}")
        data
    }

    @Override
    Map trades(String pair, Integer limit) {
        log.debug('')
        log.debug("calling trades for pair ${pair}...")

        def data = callPublicApi('trades', pair, limit)

        log.debug("data: ${data}")
        data
    }

    @Override
    Map getAccountInfo() {
        log.debug('')
        log.debug('calling getInfo...')

        String params = "method=getInfo&nonce=${yobitRepository.getNonce()}"
        def data = callTradingApi(HttpMethod.POST, params)

        log.debug("data: ${data}")
        data
    }

    @Override
    Map trade(String pair, TradeOperation type, BigDecimal rate, BigDecimal amount) {
        log.debug('')
        log.debug("calling Trade with params: pair ${pair}, type ${type}, rate ${rate}, amount ${amount}...")

        String params = "method=Trade&pair=${pair}&type=${type}&rate=${rate}&amount=${amount}&nonce=${yobitRepository.getNonce()}"
        def data = callTradingApi(HttpMethod.POST, params)

        log.debug("data: ${data}")
        data
    }

    @Override
    Map activeOrders(String pair) {
        log.debug('')
        log.debug("calling ActiveOrders for pair ${pair}...")

        String params = "method=ActiveOrders&pair=${pair}&nonce=${yobitRepository.getNonce()}"
        def data = callTradingApi(HttpMethod.POST, params)

        log.debug("data: ${data}")
        data
    }

    @Override
    Map orderInfo(Long orderId) {
        log.debug('')
        log.debug("calling OrderInfo for order ${orderId}...")

        String params = "method=OrderInfo&order_id=${orderId}&nonce=${yobitRepository.getNonce()}"
        def data = callTradingApi(HttpMethod.POST, params)

        log.debug("data: ${data}")
        data
    }

    @Override
    Map cancelOrder(Long orderId) {
        log.debug('')
        log.debug("calling CancelOrder for order ${orderId}...")

        String params = "method=CancelOrder&order_id=${orderId}&nonce=${yobitRepository.getNonce()}"
        def data = callTradingApi(HttpMethod.POST, params)

        log.debug("data: ${data}")
        data
    }
// --------------------------------------------------------------------

    private Map callPublicApi(String methodName, String currencyPair, Integer limit) {
        def apiUrl = clientProperties.getPublicApiUrl()
        def limitParam = limit != null ? "?limit=${limit}" : ''
        def url = "${apiUrl}/${methodName}/${currencyPair ?: ''}${limitParam}"
        try {
            httpClient.getForObject(url, Map.class)
        } catch (Exception e) {
            log.error("Error calling API: ${e.getMessage()}")
            [:]
        }
    }

    private Map callTradingApi(HttpMethod httpMethod, String params) {
        log.debug('')
        log.debug("params: ${params}")

        def apiUrl = clientProperties.getTradingApiUrl()
        def auth = getAuth(params)
        try {
            httpClient.exchange(apiUrl, httpMethod, auth, Map.class)?.getBody()
        } catch (Exception e) {
            log.error("Error calling API: ${e.getMessage()}")
            [:]
    }
}

    private HttpEntity<String> getAuth(String params) {
        String signature = sign(params)
        headers.put(SIGNATURE_HEADER, Collections.singletonList(signature))
        HttpEntity<String> httpEntity = new HttpEntity<>(params, headers)
        httpEntity
    }

    private String sign(String params) {
        utils.signParams(clientProperties.getSecretKey(), params)
    }
}
