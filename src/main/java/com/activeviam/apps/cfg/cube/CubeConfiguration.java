/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.cube;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.copper.avinternal.motherboard.core.configurator.IDimensionRelationship;
import com.activeviam.activepivot.copper.avinternal.motherboard.core.configurator.IManagerConfigurator;
import com.activeviam.activepivot.copper.avinternal.motherboard.core.configurator.IMeasuresConfigurator;
import com.activeviam.activepivot.copper.avinternal.motherboard.core.definition.HierarchyDefinition;
import com.activeviam.activepivot.core.impl.api.description.impl.ComparatorDescription;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IDimension;
import com.activeviam.activepivot.core.intf.api.cube.metadata.DimensionIdentifier;
import com.activeviam.activepivot.core.intf.api.cube.metadata.HierarchyIdentifier;
import com.activeviam.activepivot.core.intf.api.cube.metadata.ILevelInfo;
import com.activeviam.activepivot.server.spring.avinternal.motherboard.core.config.CustomizableActivePivotWithDatastoreConfig;
import com.activeviam.activepivot.server.spring.avinternal.motherboard.core.config.ICubeSetupHandler;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.database.api.schema.IDatabaseSchema;
import com.activeviam.database.api.schema.JoinPath;
import com.activeviam.tech.chunks.api.types.ContentType;
import com.activeviam.tech.core.api.ordering.IComparator;

@Import(value = {CustomizableActivePivotWithDatastoreConfig.class})
@Configuration
public class CubeConfiguration {

    /* *********************/
    /* OLAP Property names */
    /* *********************/
    public static final String CUBE_NAME = "Cube";

    /* ********** */
    /* Formatters */
    /* ********** */
    public static final String DOUBLE_FORMATTER = "DOUBLE[#,###.##]";
    public static final String INT_FORMATTER = "INT[#,###]";

    public static final String NATIVE_MEASURES = "Native Measures";

    public static final DimensionIdentifier TRADE_ATTRIBUTES_DIMENSION = new DimensionIdentifier("Trade Attributes");
    public static final HierarchyIdentifier COUNTERPARTY_HIERARCHY =
            TRADE_ATTRIBUTES_DIMENSION.hierarchy(COUNTERPARTY_ID);

    public static final HierarchyIdentifier ASOFDATE_HIERARCHY = new HierarchyIdentifier(ASOFDATE, ASOFDATE);

    @Bean
    ICubeSetupHandler cubeSetupHandler() {
        return CubeConfiguration::configureCube;
    }

    private static void configureCube(IManagerConfigurator configurator, IDatabaseSchema databaseSchema) {
        var pivot = configurator.createPivot(CUBE_NAME, TRADES_STORE_NAME);
        pivot.dimensions()
                .addDimension(TRADE_ATTRIBUTES_DIMENSION, TRADE_ATTRIBUTES_STORE_NAME)
                .autoAddAttributeHierarchies(
                        dataTableField -> dataTableField.getType().getContentType() != ContentType.DOUBLE,
                        ((hierarchyConfigurator, dataTableField) -> {}));
        // We need to do this to add level properties, will change later
        var asOfDateConfigurator = pivot.dimensions()
                .addDimension(
                        ASOFDATE_HIERARCHY.getDimension(),
                        TRADES_STORE_NAME,
                        new IDimensionRelationship.ManyToOneRelationship(JoinPath.of()))
                .type(IDimension.DimensionType.TIME)
                .addHierarchy(HierarchyDefinition.builder(ASOFDATE_HIERARCHY.getHierarchyName())
                        .slicing()
                        .withLevel(ASOFDATE, FieldPath.of(ASOFDATE))
                        .build());
        asOfDateConfigurator
                .getLevelConfigurator(ASOFDATE)
                .type(ILevelInfo.LevelType.TIME_DAYS)
                .comparator(new ComparatorDescription(IComparator.DESCENDING_NATURAL_ORDER_PLUGIN_KEY));

        pivot.measures()
                .addMeasure(IMeasuresConfigurator.sum(FieldPath.of(NOTIONAL))
                        .as(NOTIONAL)
                        .withFormatter(DOUBLE_FORMATTER));
        pivot.measures().addMeasure(Copper.timestamp());

        // TODO: features not implemented yet

        //        pivot.withPartialAggregateProvider(PartialProviderDefinition.createFromHierarchies(
        //                "myProvider",
        //                IAggregateProviderDefinition.BITMAP_PLUGIN_TYPE,
        //                List.of(COUNTERPARTY_HIERARCHY),
        //                List.of(NOTIONAL),
        //                new Properties()));

        //        pivot.measures().withDefaultMeasureFormatter(type -> switch (type) {
        //            case DOUBLE -> DOUBLE_FORMATTER;
        //            case INTEGER -> INT_FORMATTER;
        //            default -> IFormatter.TO_STRING_PLUGIN_KEY;
        //        });

        //        // Shared context values
        //        // Query maximum execution time (before timeout cancellation): 30s
        //        pivot.withSharedContextValue(QueriesTimeLimit.of(30, TimeUnit.SECONDS));
        //        pivot.withSharedContextValue(
        //                StartBuilding.mdxContext().aggressiveFormulaEvaluation(true).build());
        //        pivot.withSharedContextValue(
        //                StartBuilding.drillthroughProperties().withMaxRows(10_000).build());
    }

    @EventListener
    void startAtotiApplication(ApplicationStartedEvent event) {

    }
}
