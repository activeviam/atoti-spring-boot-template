/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class DataGenerator {

    private static final RandomGenerator RANDOM_GENERATOR = ThreadLocalRandom.current();
    private final int desksCount;
    private final String[] desks;
    private final int cptysCount;
    private final String[] cptys;
    private final int portfoliosCount;
    private final String[] portfolios;

    public DataGenerator(int desksCount, int cptysCount, int portfoliosCount) {
        this.desksCount = desksCount;
        this.cptysCount = cptysCount;
        this.portfoliosCount = portfoliosCount;
        desks = IntStream.range(0, desksCount)
                .mapToObj(i -> String.format("Desk_%d", i))
                .toArray(String[]::new);
        cptys = IntStream.range(0, cptysCount)
                .mapToObj(i -> String.format("Cpty_%d", i))
                .toArray(String[]::new);
        portfolios = IntStream.range(0, portfoliosCount)
                .mapToObj(i -> String.format("PF_%d", i))
                .toArray(String[]::new);
    }

    private String getDesk(int tradeId) {
        return desks[Math.floorMod(tradeId, desksCount)];
    }

    private String getCpty(int tradeId) {
        return cptys[Math.floorMod(tradeId, cptysCount)];
    }

    private String getPortfolio(int tradeId) {
        return portfolios[Math.floorMod(tradeId, portfoliosCount)];
    }

    public List<Object[]> generateTradeData(LocalDate asOfDate, int tradesCount) {
        return IntStream.range(0, tradesCount)
                .mapToObj(i -> new Object[] {asOfDate, i, RANDOM_GENERATOR.nextDouble() * 10_000})
                .toList();
    }

    public List<Object[]> generateTradeAttributesData(LocalDate asOfDate, int tradesCount) {
        return IntStream.range(0, tradesCount)
                .mapToObj(i -> new Object[] {
                    asOfDate,
                    i,
                    RANDOM_GENERATOR.nextBoolean() ? asOfDate.minusDays(RANDOM_GENERATOR.nextInt(60)) : null,
                    getCpty(i),
                    getDesk(i),
                    getPortfolio(i),
                    RANDOM_GENERATOR.nextInt(20)
                })
                .toList();
    }
}
