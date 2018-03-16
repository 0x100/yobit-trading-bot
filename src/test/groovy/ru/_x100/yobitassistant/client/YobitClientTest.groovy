package ru._x100.yobitbot.client

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import ru._x100.yobitbot.enums.TradeOperation

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@RunWith(SpringRunner)
@SpringBootTest
class YobitClientTest {

    @Autowired
    IYobitClient yobitClient

    @Test
    void ticker() {
        Map data = yobitClient.ticker('pac_rur')
        assertNotNull data
        assertNotNull data.pac_rur
    }

    @Test
    void depth() {
        Map data = yobitClient.depth('trx_rur', null)
        assertNotNull data
        assertNotNull data.trx_rur
    }

    @Test
    void trades() {
        Map trades = yobitClient.trades('doge_rur', 10)
        assertNotNull trades
        assertNotNull trades.eth_rur
    }

    @Test
    void getInfo() {
        Map accountInfo = yobitClient.getAccountInfo()
        assertNotNull accountInfo
        assertEquals(1, accountInfo.success)

        def data = accountInfo.return
        assertNotNull data
        assertNotNull data.rights
        assertNotNull data.funds
        assertNotNull data.funds_incl_orders
        assertNotNull data.server_time
    }

    @Test
    void trade() {
        def pair = 'pac_rur'
        def operation = TradeOperation.BUY
        def rate = 0.00001
        def amount = 100000.0

        Map result = yobitClient.trade(pair, operation, rate, amount)
        assertNotNull result
        assertEquals(1, result.success)
    }

    @Test
    void orderInfo() {
        Long orderId = 102380468802853

        Map orderInfo = yobitClient.orderInfo(orderId)
        assertNotNull orderInfo
        assertEquals(1, orderInfo.success)
    }

    @Test
    void cancelOrder() {
        Long orderId = 102380468802853

        Map result = yobitClient.cancelOrder(orderId)
        assertNotNull result
        assertEquals(1, result.success)
    }

    @Test
    void activeOrders() {
        def pair = 'pac_rur'

        Map result = yobitClient.activeOrders(pair)
        assertNotNull result
        assertEquals(1, result.success)
    }
}