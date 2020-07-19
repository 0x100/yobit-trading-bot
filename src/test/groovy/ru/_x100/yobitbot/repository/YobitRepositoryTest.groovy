package ru._x100.yobitbot.repository

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@RunWith(SpringRunner)
@SpringBootTest
@ActiveProfiles('test')
class YobitRepositoryTest {

    @Autowired
    TradeRepository tradeRepository

    @Test
    void getNonce() {
        Long nonce1 = tradeRepository.getNonce()
        Long nonce2 = tradeRepository.getNonce()

        assertNotNull(nonce1)
        assertNotNull(nonce2)

        assertEquals(nonce1 + 1, nonce2)
    }
}