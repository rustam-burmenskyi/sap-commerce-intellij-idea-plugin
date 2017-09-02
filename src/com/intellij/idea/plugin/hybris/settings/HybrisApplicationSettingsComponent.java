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

package com.intellij.idea.plugin.hybris.settings;

import com.intellij.idea.plugin.hybris.common.HybrisConstants;
import com.intellij.idea.plugin.hybris.statistics.StatsCollector;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.containers.HashSet;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;


/**
 * Created 19:39 29 March 2015
 *
 * @author Alexander Bartash <AlexanderBartash@gmail.com>
 */
@State(
    name = "HybrisApplicationSettings",
    storages = {
        @Storage(file = StoragePathMacros.APP_CONFIG + '/' + HybrisConstants.HYBRIS_INTEGRATION_SETTINGS_FILE_NAME)
    }
)
public class HybrisApplicationSettingsComponent implements PersistentStateComponent<HybrisApplicationSettings> {

    protected final HybrisApplicationSettings hybrisApplicationSettings = new HybrisApplicationSettings();

    @NotNull
    public static HybrisApplicationSettingsComponent getInstance() {
        return ServiceManager.getService(HybrisApplicationSettingsComponent.class);
    }

    @NotNull
    @Override
    public HybrisApplicationSettings getState() {
        return this.hybrisApplicationSettings;
    }

    @Override
    public void loadState(final HybrisApplicationSettings state) {
        XmlSerializerUtil.copyBean(state, this.hybrisApplicationSettings);
    }

    public void addUsedAction(@NotNull final StatsCollector.ACTIONS action) {
        final HashSet<StatsCollector.ACTIONS> usedActions = hybrisApplicationSettings.getUsedActions();
        usedActions.add(action);
        hybrisApplicationSettings.setUsedActions(usedActions);
    }
}
