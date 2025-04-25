/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest.query;

import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;

public interface LevelsConverter {

    LevelIdentifier stringToLevelIdentifier(String level);
}
