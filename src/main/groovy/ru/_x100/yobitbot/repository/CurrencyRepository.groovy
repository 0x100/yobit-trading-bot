package ru._x100.yobitbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru._x100.yobitbot.enums.CurrencyStatus
import ru._x100.yobitbot.model.entity.CurrencyInfo

interface CurrencyRepository extends JpaRepository<CurrencyInfo, Integer> {

    CurrencyInfo findOneByPair(String pair)

    List<CurrencyInfo> findByStatus(CurrencyStatus status)
}