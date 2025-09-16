/*
 ******************************************************************************
 * (C) ActiveViam 2018-2024                                                   *
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY     *
 * property of ActiveViam. Any unauthorized use,                              *
 * reproduction or transfer of this material is strictly prohibited           *
 ******************************************************************************
 */
package com.activeviam.apps.cfg.pivot.hierarchies;

import java.io.Serial;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.lang.NonNull;

import com.activeviam.accelerator.common.hierarchy.SingleLevelHierarchyDescription;
import com.activeviam.activepivot.core.ext.api.cube.hierarchy.impl.AAnalysisHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IAnalysisHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.metadata.IAnalysisHierarchyInfo;
import com.activeviam.activepivot.core.intf.api.cube.metadata.ILevelInfo;
import com.activeviam.apps.cfg.pivot.CubeConstants;
import com.activeviam.database.api.IDatabaseVersion;
import com.activeviam.tech.chunks.api.types.Types;
import com.activeviam.tech.core.api.ordering.CustomComparator;
import com.activeviam.tech.core.api.registry.AtotiExtendedPluginValue;
import com.activeviam.tech.dictionaries.api.IDictionary;

/**
 * This will create an analysis hierarchy with all the available currencies
 */
@AtotiExtendedPluginValue(intf = IAnalysisHierarchy.class, key = DisplayCurrencyHierarchy.PLUGIN_KEY)
public class DisplayCurrencyHierarchy extends AAnalysisHierarchy {

    /**
     * Return a description object to setup the hierarchy
     * @param hierarchy The level to create
     * @return The description
     */
    public static @NonNull SingleLevelHierarchyDescription.Builder<Description> hierarchy(@NonNull String hierarchy) {
        return new SingleLevelHierarchyDescription.Builder<>(Description::new, hierarchy);
    }

    /** serialVersionUID. */
    @Serial
    private static final long serialVersionUID = 65165465465421L;

    /** Plugin type */
    public static final String PLUGIN_KEY = "DISPLAY_CURRENCY_HIERARCHY";

    private Set<String> currencies;

    public static class Description extends SingleLevelHierarchyDescription<Description> {

        private Description(@NonNull String hierarchy, @NonNull String level) {
            super(PLUGIN_KEY, hierarchy, level);
            withType(Types.TYPE_STRING);
        }

        /**
         * The order of the currencies
         * @param firstCCy The list of ccy to order
         * @return itself
         */
        public Description withCurrenciesOrder(@NonNull String... firstCCy) {
            return withCurrenciesOrder(Arrays.asList(firstCCy));
        }

        /**
         * The order of the currencies
         * @param firstCCy The list of ccy to order
         * @return itself
         */
        public Description withCurrenciesOrder(@NonNull List<String> firstCCy) {
            withComparator(new CustomComparator<>(firstCCy, List.of()));
            return this;
        }
    }

    /**
     * Constructor
     *
     * @param info the info about the hierarchy
     */
    public DisplayCurrencyHierarchy(IAnalysisHierarchyInfo info, final List<ILevelInfo> levelInfos, IDictionary<Object>[] levelDictionaries) {
        super(info, levelInfos, levelDictionaries);
        currencies = Set.of(CubeConstants.DISPLAY_CURRENCIES.split(","));
    }

    /**
     * Will build the levels members.
     * @return The members
     */
    @Override
    protected Iterator<Object[]> buildDiscriminatorPathsIterator(IDatabaseVersion databaseVersion) {
        Function<Object, Object[]> converter = isAllMembersEnabled ?
                ccy -> new Object[] { ALLMEMBER, ccy } :
                ccy -> new Object[] { ccy };
        return currencies.stream().distinct().map(converter).iterator();
    }

    @Override
    public String getType() {
        return PLUGIN_KEY;
    }
}
