package ru._x100.yobitbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru._x100.yobitbot.enums.TradeStatus
import ru._x100.yobitbot.model.entity.Trade

interface TradeRepository extends JpaRepository<Trade, Integer> {

    List<Trade> findByPairAndStatusIn(String pair, List<TradeStatus> statuses)

    List<Trade> findByPairAndStatusAndSellPrice(String pair, TradeStatus status, BigDecimal sellPrice)

    List<Trade> findByPairAndStatusAndBuyPrice(String pair, TradeStatus status, BigDecimal buyPrice)

    @Query("SELECT sum(t.profit) FROM Trade t WHERE t.status = ?1")
    BigDecimal getTotalProfit(TradeStatus status)

    @Query("SELECT sum(t.amount * t.buyPrice) FROM Trade t WHERE t.status IN ?1 AND t.pair IN ?2")
    BigDecimal getTradingSum(List<TradeStatus> statuses, List<String> currencyPairs)

    @Query("SELECT sum(t.amount * t.buyPrice) FROM Trade t WHERE t.pair = ?1 AND t.status IN ?2")
    BigDecimal getTradingSum(String currencyPair, List<TradeStatus> statuses)

    @Query("SELECT sum(t.amount) FROM Trade t WHERE t.pair = ?1 AND t.status IN ?2")
    BigDecimal getTradingAmount(String currencyPair, List<TradeStatus> statuses)

    @Query(value = "SELECT sq_nonce.nextval FROM dual", nativeQuery = true)
    Long getNonce()
}