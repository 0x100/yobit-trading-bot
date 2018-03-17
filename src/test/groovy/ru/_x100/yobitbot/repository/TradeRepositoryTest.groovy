package ru._x100.yobitbot.repository

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import ru._x100.yobitbot.enums.TradeStatus
import ru._x100.yobitbot.model.entity.Trade

import java.time.LocalDateTime

import static org.junit.Assert.*

@RunWith(SpringRunner)
@SpringBootTest
@Transactional
@ActiveProfiles('test')
class TradeRepositoryTest {

    @Autowired
    private TradeRepository tradeRepository

    @Test
    void save() {
        Trade savedTrade = createTrade()

        assertNotNull savedTrade
        assertNotNull savedTrade.id

        assertEquals('DOGE', savedTrade.pair)
        assertEquals(5000.12345678, savedTrade.amount)
        assertEquals(0.001234, savedTrade.buyPrice)
        assertTrue savedTrade.buyDate < LocalDateTime.now()
        assertEquals(0.002345, savedTrade.sellPrice)
        assertTrue savedTrade.sellDate > savedTrade.buyDate
        assertEquals(100.0, savedTrade.profit)
        assertEquals(TradeStatus.SOLD, savedTrade.status)
    }

    @Test
    void findOneByPairAndStatusIn() {
        Trade createdTrade = createTrade()
        List<Trade> trades = tradeRepository.findByPairAndStatusIn('DOGE', Collections.singletonList(TradeStatus.SOLD))
        Trade trade = trades.first()
        assertEquals(createdTrade.id, trade.id)
    }

    @Test
    void getTradedSum() {
        createTrade()
        assertTrue(tradeRepository.getTradingSum('DOGE', Arrays.asList(TradeStatus.SOLD, TradeStatus.PURCHASED)) > 0.0)
        assertNull(tradeRepository.getTradingSum('DOGE', Arrays.asList(TradeStatus.ACTIVE, TradeStatus.PAUSE)))
    }

    private Trade createTrade() {
        Trade trade = [
                pair     : 'DOGE',
                amount   : 5000.12345678,
                buyPrice : 0.001234,
                buyDate  : LocalDateTime.now(),
                sellPrice: 0.002345,
                sellDate : LocalDateTime.now().plusHours(1L),
                profit   : 100.0,
                status   : TradeStatus.SOLD
        ]
        tradeRepository.save(trade) as Trade
    }
}