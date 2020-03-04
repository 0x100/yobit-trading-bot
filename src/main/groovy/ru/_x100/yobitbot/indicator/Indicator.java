package ru._x100.yobitbot.indicator;
import ru._x100.yobitbot.model.Candle;

import java.util.List;
import java.util.stream.Collectors;

public interface Indicator {

    double calculate(List<Candle> candles);

    default List<Candle> limitCandles(List<Candle> candles, int periodsCount) {
        return candles.stream()
                .limit(periodsCount)
                .collect(Collectors.toList());
    }
}
