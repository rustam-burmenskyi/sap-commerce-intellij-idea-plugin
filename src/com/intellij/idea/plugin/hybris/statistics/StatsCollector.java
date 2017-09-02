/*
 * This file is part of "hybris integration" plugin for Intellij IDEA.
 * Copyright (C) 2014-2016 Alexander Bartash <AlexanderBartash@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.intellij.idea.plugin.hybris.statistics;

import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Martin Zdarsky-Jones (martin.zdarsky@hybris.com) on 28/2/17.
 */
public interface StatsCollector {

    void collectStat(@NotNull ACTIONS action, @Nullable String parameters);

    enum ACTIONS {
        IMPORT_PROJECT,
        REFRESH_PROJECT,
        OPEN_PROJECT,
        ANT,
        OPEN_POTENTIAL_PROJECT,
        BUSINESS_DIAGRAM,
        DEPENDENCY_DIAGRAM,
        IMPEX_TABLE_FORMAT,
        BUILD_PROJECT,
        RUN_TESTS
    }

    void collectStat(@NotNull ACTIONS action);

    @NotNull
    static StatsCollector getInstance() {
        return ServiceManager.getService(StatsCollector.class);
    }
}
