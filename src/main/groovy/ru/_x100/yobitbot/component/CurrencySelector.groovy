package ru._x100.yobitbot.component

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru._x100.yobitbot.client.IYobitClient
import ru._x100.yobitbot.config.BotConfig
import ru._x100.yobitbot.config.YobitApiConfig
import ru._x100.yobitbot.enums.CurrencyStatus
import ru._x100.yobitbot.model.entity.CurrencyInfo
import ru._x100.yobitbot.repository.CurrencyRepository


@Component
@Slf4j
class CurrencySelector {

    @Autowired
    IYobitClient client

    @Autowired
    YobitApiConfig yobitApiConfig

    @Autowired
    BotConfig botConfig

    @Autowired
    CurrencyRepository currencyRepository

    @Autowired
    TradeBot tradeBot

    @Scheduled(fixedDelayString = '${bot.refreshCurrenciesListInterval}', initialDelay = 3600000L)
    void refreshCurrencyList() {

        List info = (client.info().pairs as Map).keySet().stream()
                .map { it.toString() }
                .collect()
                .collate(40)

        List pairs = []
        for (List data : info) {
            Map result = client.ticker(data.join('-'))
            Thread.sleep(250)
            if(!result.keySet().contains('success') || result.success != 0) {
                List selectedCurrencies = result.entrySet().stream()
                        .filter { it.value.'vol' > botConfig.currencyVolumeThreshold &&
                            it.value.'buy' < botConfig.currencyFilterThreshold &&
                            it.key.toString().endsWith("_${botConfig.baseCurrency.toLowerCase()}") }
                        .map { it.key.toString() }
                        .collect()
                pairs.addAll(selectedCurrencies)
            }
        }
        for (CurrencyInfo currencyInfo : currencyRepository.findAll()) {
            currencyInfo.status = CurrencyStatus.BLOCKED
            currencyRepository.saveAndFlush(currencyInfo)
        }
        Set<String> activeCurrencies = pairs.stream()
                .map { it.toString().split('_')[0].toUpperCase() }
                .collect()
        if(botConfig.currencies) {
            activeCurrencies.addAll(new LinkedList<String>(botConfig.currencies))
        }

        log.info("Selected currencies: ${activeCurrencies}")

        for (String currency : activeCurrencies) {
            if(!botConfig.bannedCurrencies?.contains(currency)) {
                CurrencyInfo currencyInfo = currencyRepository.findOneByPair(currency)
                if (currencyInfo == null) {
                    currencyInfo = new CurrencyInfo(currency)
                }
                currencyInfo.status = CurrencyStatus.ACTIVE
                currencyRepository.saveAndFlush(currencyInfo)
            }
        }
        tradeBot.init()
    }
}
