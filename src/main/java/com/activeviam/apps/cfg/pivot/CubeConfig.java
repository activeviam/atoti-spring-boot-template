package com.activeviam.apps.cfg.pivot;

import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.desc.build.ICanBuildCubeDescription;
import com.activeviam.desc.build.ICubeDescriptionBuilder;
import com.activeviam.desc.build.dimensions.ICanStartBuildingDimensions;
import com.quartetfs.biz.pivot.cube.dimension.IDimension;
import com.quartetfs.biz.pivot.cube.hierarchy.ILevelInfo;
import com.quartetfs.biz.pivot.definitions.IActivePivotInstanceDescription;
import com.quartetfs.fwk.ordering.impl.ReverseOrderComparator;

import static com.activeviam.apps.cfg.pivot.PivotManagerConfig.INT_FORMATTER;
import static com.activeviam.apps.cfg.pivot.PivotManagerConfig.NATIVE_MEASURES;

public class CubeConfig {

    /**
     * Configures the given builder in order to created the cube description.
     *
     * @param builder The builder to configure
     * @return The configured builder
     */
    public static ICanBuildCubeDescription<IActivePivotInstanceDescription> configureCubeBuilder(
            final ICubeDescriptionBuilder.INamedCubeDescriptionBuilder builder) {

        return builder
                .withContributorsCount()
                .withinFolder(NATIVE_MEASURES)
                .withAlias("Count")
                .withFormatter(INT_FORMATTER)

                .withCalculations(Measures::build)
                .withDimensions(CubeConfig::dimensions)

                // Aggregate provider
                .withAggregateProvider()
                .jit()

                // Shared context values
                // Query maximum execution time (before timeout cancellation): 1h
                .withSharedMdxContext().aggressiveFormulaEvaluation(true).end()
                .withSharedDrillthroughProperties()
                .withMaxRows(10000)
                .end()

                ;
    }

    /**
     * Adds the dimensions descriptions to the input
     * builder.
     *
     * @param builder The cube builder
     * @return The builder for chained calls
     */
    public static ICanBuildCubeDescription<IActivePivotInstanceDescription> dimensions(ICanStartBuildingDimensions builder) {

        return builder
                .withSingleLevelDimensions(
                        StoreAndFieldConstants.TRADES__TRADEID
                )
                .withDimension(StoreAndFieldConstants.ASOFDATE)
                .withType(IDimension.DimensionType.TIME)
                .withHierarchy(StoreAndFieldConstants.ASOFDATE).slicing()
                .withLevelOfSameName()
                .withType(ILevelInfo.LevelType.TIME).withComparator(ReverseOrderComparator.type)
                ;

    }

}