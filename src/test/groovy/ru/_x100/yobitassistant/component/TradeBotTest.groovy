package ru._x100.yobitbot.component

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.junit4.SpringRunner
import ru._x100.yobitbot.client.impl.YobitClient
import ru._x100.yobitbot.config.YobitApiConfig
import ru._x100.yobitbot.enums.Action
import ru._x100.yobitbot.enums.TradeStatus
import ru._x100.yobitbot.enums.TrendType
import ru._x100.yobitbot.model.entity.Trade
import ru._x100.yobitbot.repository.CurrencyRepository
import ru._x100.yobitbot.repository.TradeRepository

import static org.junit.Assert.assertEquals

@RunWith(SpringRunner)
@SpringBootTest
class TradeBotTest {

    @SpyBean
    TradeBot tradeBot

    @MockBean
    YobitApiConfig yobitApiConfig

    @MockBean
    YobitClient yobitClient

    @MockBean
    TradeRepository tradeRepository

    @MockBean
    CurrencyRepository currencyRepository

    @Before
    void setUp() {
        tradeBot.prevAdvice = [:]
    }

    @Test
    void getTrendType1() {
        def trades = [
                [price: 0.005, type: 'bid'],
                [price: 0.004, type: 'bid'],
                [price: 0.003, type: 'bid'],
                [price: 0.002, type: 'bid'],
                [price: 0.001, type: 'bid']
        ]
        def trendType = tradeBot.getTrendType(trades)
        assertEquals(TrendType.UP, trendType)
    }

    @Test
    void getTrendType2() {
        def trades = [
                [price: 0.005, type: 'bid'],
                [price: 0.004, type: 'bid'],
                [price: 0.003, type: 'ask'],
                [price: 0.004, type: 'ask'],
                [price: 0.005, type: 'ask']
        ]
        def trendType = tradeBot.getTrendType(trades)
        assertEquals(TrendType.MOTIONLESS, trendType)
    }

    @Test
    void getTrendType3() {
        def trades = [
                [price: 0.001, type: 'ask'],
                [price: 0.002, type: 'ask'],
                [price: 0.003, type: 'ask'],
                [price: 0.004, type: 'ask'],
                [price: 0.005, type: 'ask']
        ]
        def trendType = tradeBot.getTrendType(trades)
        assertEquals(TrendType.DOWN, trendType)
    }

    @Test
    void getTrendType4() {
        def trades = [
                [price: 0.001, type: 'bid'],
                [price: 0.002, type: 'bid'],
                [price: 0.003, type: 'bid'],
                [price: 0.004, type: 'bid'],
                [price: 0.005, type: 'bid']
        ]
        def trendType = tradeBot.getTrendType(trades)
        assertEquals(TrendType.DOWN, trendType)
    }

    @Test
    void getTrendType5() {
        def trades = [
                [price: 0.005, type: 'ask'],
                [price: 0.004, type: 'ask'],
                [price: 0.003, type: 'ask'],
                [price: 0.002, type: 'ask'],
                [price: 0.001, type: 'ask']
        ]
        def trendType = tradeBot.getTrendType(trades)
        assertEquals(TrendType.UP, trendType)
    }

    @Test
    void getTrendType6() {
        def trades = [
                [price: 0.006, type: 'bid'],
                [price: 0.005, type: 'bid'],
                [price: 0.003, type: 'ask'],
                [price: 0.004, type: 'ask'],
                [price: 0.005, type: 'ask']
        ]
        def trendType = tradeBot.getTrendType(trades)
        assertEquals(TrendType.UP, trendType)
    }

    @Test
    void getTrendType7() {
        def trades = [
                [price: 0.010, type: 'ask'],
                [price: 0.010, type: 'ask'],
                [price: 0.012, type: 'ask'],
                [price: 0.014, type: 'ask'],
                [price: 0.015, type: 'ask']
        ]
        def trendType = tradeBot.getTrendType(trades)
        assertEquals(TrendType.DOWN, trendType)
    }

    @Test
    void getTrendType8() {
        def trades = [
                [price: 0.010, type: 'bid'],
                [price: 0.009, type: 'bid'],
                [price: 0.007, type: 'ask'],
                [price: 0.009, type: 'bid'],
                [price: 0.008, type: 'ask']
        ]
        def trendType = tradeBot.getTrendType(trades)
        assertEquals(TrendType.UP, trendType)
    }

    @Test
    void getAdvice1() {

        def tradeRecord = new Trade()
        tradeRecord.status = TradeStatus.ACTIVE

        def trades1 = [
                [price: 0.010, type: 'ask'],
                [price: 0.010, type: 'ask'],
                [price: 0.012, type: 'ask'],
                [price: 0.014, type: 'ask'],
                [price: 0.015, type: 'ask']
        ]
        def currency24hInfo = [
                avg: 0.013,
                buy: 0.009
        ]
        tradeBot.getAdvice(trades1, tradeRecord, currency24hInfo)


        def trades2 = [
                [price: 0.001, type: 'ask'],
                [price: 0.002, type: 'ask'],
                [price: 0.003, type: 'ask'],
                [price: 0.003, type: 'ask'],
                [price: 0.008, type: 'ask']
        ]
        currency24hInfo.buy = 0.0009
        def advice = tradeBot.getAdvice(trades2, tradeRecord, currency24hInfo)
        assertEquals(Action.WAIT_4_BUY, advice.action)
    }

    @Test
    void getAdvice2() {

        def trades1 = [
                [price: 0.001, type: 'ask'],
                [price: 0.002, type: 'ask'],
                [price: 0.003, type: 'ask'],
                [price: 0.003, type: 'ask'],
                [price: 0.008, type: 'ask']
        ]
        tradeBot.getAdvice(trades1, new Trade(), [:])

        def trades2 = [
                [price: 0.010, type: 'ask'],
                [price: 0.010, type: 'ask'],
                [price: 0.012, type: 'ask'],
                [price: 0.014, type: 'ask'],
                [price: 0.015, type: 'bid']
        ]

        def advice = tradeBot.getAdvice(trades2, new Trade(), [:])
        assertEquals(Action.WAIT, advice.action)
    }

    @Test
    void getAdvice3() {

        def trades1 = [
                [price: 0.012, type: 'ask'],
                [price: 0.014, type: 'ask'],
                [price: 0.016, type: 'ask'],
                [price: 0.017, type: 'ask'],
                [price: 0.018, type: 'ask']
        ]
        tradeBot.getAdvice(trades1, new Trade(), [:])

        def trades2 = [
                [price: 0.010, type: 'ask'],
                [price: 0.010, type: 'ask'],
                [price: 0.011, type: 'ask'],
                [price: 0.012, type: 'ask'],
                [price: 0.013, type: 'ask']
        ]
        tradeBot.getAdvice(trades2, new Trade(), [:])

        def trades3 = [
                [price: 0.010, type: 'bid'],
                [price: 0.009, type: 'ask'],
                [price: 0.010, type: 'ask'],
                [price: 0.012, type: 'ask'],
                [price: 0.013, type: 'ask']
        ]
        tradeBot.getAdvice(trades3, new Trade(), [:])

        def trades4 = [
                [price: 0.015, type: 'bid'],
                [price: 0.013, type: 'bid'],
                [price: 0.012, type: 'ask'],
                [price: 0.013, type: 'ask'],
                [price: 0.014, type: 'ask']
        ]

        def advice = tradeBot.getAdvice(trades4, new Trade(), [avg: 13.972])
        assertEquals(Action.BUY, advice.action)
    }
}