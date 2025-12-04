/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.activeviam.activepivot.core.impl.api.query.GetAggregatesQuery;
import com.activeviam.activepivot.core.intf.api.contextvalues.IContextSnapshot;
import com.activeviam.activepivot.core.intf.api.contextvalues.subcube.ISubCubeProperties;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.IMultiVersionActivePivot;
import com.activeviam.activepivot.core.intf.api.cube.metadata.HierarchyIdentifier;
import com.activeviam.activepivot.core.intf.api.query.IGetAggregatesQuery;
import com.activeviam.activepivot.core.intf.api.query.drillthrough.IDrillthroughQuery;
import com.activeviam.activepivot.server.impl.private_.webservices.QueriesService;
import com.activeviam.activepivot.xmla.pivot.private_.mdx.utils.impl.ServicesUtil;
import com.activeviam.apps.soap.dto.VarDataExtractDTO;
import com.activeviam.apps.soap.dto.VarDealValueDTO;
import com.activeviam.apps.soap.dto.VarDealVectorDTO;
import com.activeviam.apps.soap.dto.VarDrillthroughDTO;
import com.activeviam.apps.soap.dto.VarQueryDTO;
import com.activeviam.tech.chunks.api.vectors.IVector;

import jakarta.annotation.PostConstruct;
import jakarta.jws.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@WebService(
        name = "IVaRService",
        targetNamespace = "http://www.quartetfs.com",
        endpointInterface = "com.activeviam.apps.soap.IVaRQueryService",
        serviceName = "VaRQueryService")
@Service
@RequiredArgsConstructor
@Slf4j
public class VaRQueryService implements IVaRQueryService {
    private static final List<Double> CONFIDENCE_LEVELS = Arrays.asList(0d, 0.01d, 0.025d, 0.975d, 0.99d, 1d);
    private static final String COB_DATE_DIM_NAME = "COB Date";

    private final IActivePivotManager activePivotManager;
    private QueriesService queriesService;

    @PostConstruct
    public void init() {
        queriesService = new QueriesService();
        queriesService.setManager(activePivotManager);
    }

    //    protected static ILocation stringToLocation(String stringLocation) {
    //        String[] levels = stringLocation.split(Pattern.quote(ILocation.HIERARCHY_SEPARATOR));
    //        String[][] results = new String[levels.length][];
    //        // override the container in order to target container
    //        //	results[0] = new String[2];
    //        //	results[0][0] = ILevel.ALLMEMBER;
    //        //	results[0][1] = containerName;
    //
    //        // then loop over other levels
    //        for (int i = 0; i < levels.length; i++) {
    //            String[] members = levels[i].split(Pattern.quote(ILocation.LEVEL_SEPARATOR), -1);
    //            results[i] = new String[members.length];
    //            for (int j = 0; j < members.length; j++) {
    //                if (members[j].equals("null")) {
    //                    results[i][j] = null;
    //                } else {
    //                    results[i][j] = members[j];
    //                }
    //            }
    //        }
    //        return new Location(results);
    //    }

    @Override
    public VarDataExtractDTO testVarDataExtract(VarQueryDTO varQueryDTO) {
        var result = new VarDataExtractDTO();
        result.setScenarioIndex(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        result.setAttributesHeader(Set.of("DealNum", "DealType"));
        result.setDeals(List.of(
                new VarDealVectorDTO(
                        "Deal1",
                        new double[] {10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0},
                        List.of("TypeA")),
                new VarDealVectorDTO(
                        "Deal2",
                        new double[] {15.0, 25.0, 35.0, 45.0, 55.0, 65.0, 75.0, 85.0, 95.0, 105.0},
                        List.of("TypeB"))));
        return result;
    }

    @Override
    public VarDataExtractDTO varDataExtract(VarQueryDTO varQueryDTO) {
        var headerProperties = new LinkedHashMap<String, Integer>();
        var result = new VarDataExtractDTO();

        try {
            // retrieve and check some params
            if (varQueryDTO == null) {
                log.error("VarQueryDTO is null.");
                return result;
            }
            var query = varQueryDTO.getDtQuery().toCoreDrillthroughQuery();
            if (query == null) {
                log.error("Underlying drillthrough query is null.");
                return result;
            }

            // check from and to
            var from = varQueryDTO.getFrom();
            var to = varQueryDTO.getTo();
            if (!(from == to && from == 0)) {
                if (!isValid(from, to)) {
                    log.error("From and to are not consistent, from:[{}] to:[{}]", from, to);
                    return result;
                }
            }

            // Execute the query contextually
            if (isContextInvalid(query)) {
                return result;
            }

            var results = queriesService.execute(query);
            if (results == null || results.getRows().isEmpty()) {
                log.info("[{}] Results size:{}.", " varDataExtract : ", 0);
                return result;
            }

            // trace
            log.info(
                    "[{}] Results size:{}.",
                    " varDataExtract : ",
                    results.getRows().size());

            // get client header
            var includeFields = Arrays.asList(StringUtils.split(varQueryDTO.getDtHeader(), "|"));

            // set headers
            var headerPosIndex = 0;
            for (var header : results.getHeaders()) {
                var name = header.getName();
                if (includeFields.contains(name)) {
                    headerProperties.putIfAbsent(name, headerPosIndex);
                }
                headerPosIndex++;
            }
            result.setAttributesHeader(headerProperties.keySet());

            // build and populate the DTO collection and align it with client headers
            List<VarDealVectorDTO> deals = new ArrayList<>(results.getRows().size());
            // To store Vector length for different VaR, i.e: VAR_1D & VAR_10D = 500
            // VAR_STRESS = 260 VAR_1540 OR VAR_SIX_YEARS = 1500
            var vectorLength = 0;

            for (var row : results.getRows()) {
                double[] vector = null;
                String dealNumber;
                List<Object> attributeValue = new ArrayList<>();
                dealNumber = (String)
                        row.getContent()[headerProperties.get("DealNum")]; // DatastoreDescriptionConfig.DealNum)];
                for (var index : headerProperties.values()) {
                    if (index == 0) {
                        continue;
                    }
                    var data = row.getContent()[index];
                    if (data instanceof IVector v) {
                        vector = v.toDoubleArray();
                        vectorLength = v.size();
                    } else if (data instanceof String) {
                        attributeValue.add(data);
                    }
                }
                if (dealNumber != null && vector != null) {
                    deals.add(new VarDealVectorDTO(dealNumber, extractSubVector(vector, from, to), attributeValue));
                }
            }
            // set the deals
            result.setDeals(deals);
            if ((from == to && from == 0)) {
                result.setScenarioIndex(rangeInt(from + 1, vectorLength));
            } else if (from == to) {
                result.setScenarioIndex(rangeInt(from, to));
            }
            // trace
            log.info(
                    "[{}] Results size:{}.",
                    " varDataExtractResults : ",
                    result.getDeals().size());
        } catch (Exception e) {
            log.error("WebService issue while calling the method varDataExtract.", e);
        }
        return result;
    }

    @Override
    public VarDrillthroughDTO varDrillthrough(VarQueryDTO varQueryDTO) {
        var result = new VarDrillthroughDTO();
        IMultiVersionActivePivot pivot = null;
        IContextSnapshot oldContext = null;
        var headerProperties = new LinkedHashMap<String, Integer>();

        try {
            // retrieve and check some params
            if (varQueryDTO == null) {
                log.error("varQueryDTO is null.");
                return result;
            }
            var query = varQueryDTO.getDtQuery().toCoreDrillthroughQuery();
            if (query == null) {
                log.error("Underlying drillthrough query is null.");
                return result;
            }

            // check the pivot
            pivot = queriesService.queriesExecutor.getActivePivots().get(query.getPivotId());

            // get the location
            var queryLocations = query.getLocations();
            if (queryLocations == null || queryLocations.size() != 1) {
                log.error("Only a list containing one location is expected.");
                return result;
            }
            var location =
                    queryLocations.iterator().next(); // get the first location, we're supposed to receive only one
            log.info("location:{}", location);
            //            var locationToQuery = stringToLocation(location);
            if (location.isRange()) {
                log.error("Range location forbidden.");
                return result;
            }
            // convert the location: date member will be converted to IDate
            //            ConvertLocationsResult conversionResult =
            //                    LocationUtil.convertLocations(pivot.getHierarchies(),
            // Collections.singletonList(locationToQuery));
            //            Set<ILocation> locations = conversionResult.getConvertedLocations();

            // confidenceLevel
            var confidenceLevel = varQueryDTO.getConfidenceLevel();
            if (!CONFIDENCE_LEVELS.contains(Double.valueOf(confidenceLevel))) {
                log.error("Wrong confidenceLevel,available values are:{}.", CONFIDENCE_LEVELS);
                return result;
            }

            // Execute the query contextually
            if (isContextInvalid(query)) {
                return result;
            }
            oldContext = ServicesUtil.applyContextValues(pivot, query.getContextValues(), true);

            var results = queriesService.execute(query);

            if (results == null || results.getRows().isEmpty()) {
                log.info("[{}] Results size:{}.", " varDrillthrough : ", 0);
                return result;
            }
            // trace
            log.info(
                    "[{}] Results size:{}.",
                    " varDrillthrough : ",
                    results.getRows().size());

            //    Collection<ILocation> locations = query.getLocations();
            // get the vector, notice that the measures should not be hidden else we can not retrieve them

            IGetAggregatesQuery gaQuery = new GetAggregatesQuery(List.of(location), List.of("VaRResult.SUM"));
            var cellSet = pivot.execute(gaQuery);
            double[] vector = null;
            var adv = cellSet.getCellValue(location, "VaRResult.SUM");
            if (adv instanceof IVector v) {
                vector = v.toDoubleArray();
            }
            if (vector == null) {
                log.info("The retrieved vector is null for the location:{}.", location);
                return result;
            }

            // get client header
            var includeFields = Arrays.asList(StringUtils.split(varQueryDTO.getDtHeader(), "|"));

            // set headers
            var headers = results.getHeaders();
            var headerPosIndex = 0;
            for (var header : headers) {
                var name = header.getName();
                if (includeFields.contains(name)) {
                    headerProperties.putIfAbsent(name, headerPosIndex);
                }
                headerPosIndex++;
            }
            result.setAttributesHeader(headerProperties.keySet());

            // get the index that matches the confidenceLevel
            int scenarioPositionIndex =
                    999999; // MarketRiskUtils.getIndexFromVectorLength(vector.length, confidenceLevel);
            var scenarioName = retrieveScenarioName(vector, scenarioPositionIndex); // retrieve the scenario name
            result.setScenarioIndex(scenarioName); // set the scenario index

            // build and populate the DTO collection and align it with client headers
            List<VarDealValueDTO> deals = new ArrayList<>(results.getRows().size());
            for (var row : results.getRows()) {
                double[] childVector = null;
                List<Object> attributeValue = new ArrayList<>();
                var dealNumber = (String)
                        row.getContent()[headerProperties.get("DealNum")]; // DatastoreDescriptionConfig.DealNum)];
                for (var index : headerProperties.values()) {
                    if (index == 0) {
                        continue;
                    }
                    var data = row.getContent()[index];
                    if (data instanceof IVector v) {
                        childVector = v.toDoubleArray();
                    } else if (data instanceof String) {
                        attributeValue.add(data);
                    }
                }
                if (dealNumber != null && childVector != null) {
                    // -1 here because scenario names are from 1 to 500 however
                    // vector offsets are from 0 to 499
                    deals.add(new VarDealValueDTO(dealNumber, childVector[scenarioName - 1], attributeValue));
                }
            }
            // set the deals
            result.setDeals(deals);

            // trace
            log.info(
                    "[{}] Size to send to the WS client:{}.",
                    "varDrillthroughtResults",
                    result.getDeals().size());
        } catch (Exception e) {
            log.error("WebService issue while calling the method varDrillthrough.", e);
        } finally {
            if (oldContext != null) {
                ServicesUtil.replaceContextValues(pivot, oldContext);
            }
        }
        return result;
    }

    protected boolean isContextInvalid(IDrillthroughQuery dtQuery) {
        if (dtQuery.getContextValues() == null) {
            return false;
        }
        for (var contextValue : dtQuery.getContextValues()) {
            if (contextValue instanceof ISubCubeProperties scp) {
                var members = scp.getGrantedMembers(HierarchyIdentifier.simple(COB_DATE_DIM_NAME));
                if (members != null && !members.isEmpty() && members.size() > 1) {
                    log.error("Multiselect not allowed for COB Date dimension.");
                    return true;
                }
            }
        }
        return false;
    }

    private int retrieveScenarioName(double[] vector, int index) {
        // populate the Pair[] array with vector, we preserve the original index after sorting Pair[] array
        var vectorSorted = new Pair[vector.length];
        for (var i = 0; i < vector.length; i++) {
            // +1 for i here because scenario name 1 is the one you have at offset zero
            vectorSorted[i] = new Pair(i + 1, vector[i]);
        }
        Arrays.sort(vectorSorted);
        return vectorSorted[index].index(); // getIndex returns the scenario name
    }

    private double[] extractSubVector(double[] originalVector, int from, int to) {
        double[] result;
        if (from < 1 && to < 1) {
            return originalVector;
        } else {
            result = new double[to - from + 1];
            System.arraycopy(originalVector, from - 1, result, 0, result.length);
        }
        return result;
    }

    private boolean isValid(int from, int to) {
        return true;
    }

    private int[] rangeInt(int from, int to) {
        return IntStream.rangeClosed(from, to).toArray();
    }
}
