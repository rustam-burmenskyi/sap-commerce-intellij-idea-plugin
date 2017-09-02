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

package com.intellij.idea.plugin.hybris.project.components;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.plugin.hybris.ant.HybrisAntBuildListener;
import com.intellij.idea.plugin.hybris.common.HybrisConstants;
import com.intellij.idea.plugin.hybris.common.services.CommonIdeaService;
import com.intellij.idea.plugin.hybris.common.utils.HybrisI18NBundleUtils;
import com.intellij.idea.plugin.hybris.common.utils.HybrisIcons;
import com.intellij.idea.plugin.hybris.project.actions.ProjectRefreshAction;
import com.intellij.idea.plugin.hybris.project.wizard.PermissionToSendStatisticsDialog;
import com.intellij.idea.plugin.hybris.settings.HybrisProjectSettings;
import com.intellij.idea.plugin.hybris.settings.HybrisProjectSettingsComponent;
import com.intellij.idea.plugin.hybris.statistics.StatsCollector;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.spring.settings.SpringGeneralSettings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.intellij.idea.plugin.hybris.project.utils.PluginCommon.ANT_SUPPORT_PLUGIN_ID;
import static com.intellij.idea.plugin.hybris.project.utils.PluginCommon.SPRING_PLUGIN_ID;
import static com.intellij.idea.plugin.hybris.project.utils.PluginCommon.isPluginActive;
import static com.intellij.openapi.util.io.FileUtilRt.toSystemDependentName;

/**
 * Created by Martin Zdarsky-Jones on 29/09/2016.
 */
public class HybrisProjectManagerListener implements ProjectManagerListener {

    private static final Logger LOG = Logger.getInstance(HybrisProjectManagerListener.class);
    @Override
    public void projectOpened(final Project project) {
        if (isOldHybrisProject(project)) {
            showNotification(project);
        }
        final CommonIdeaService commonIdeaService = ServiceManager.getService(CommonIdeaService.class);
        if (!commonIdeaService.isHybrisProject(project)) {
            return;
        }
        if (popupPermissionToSendStatistics(project)) {
            continueOpening(project);
        }
    }

    private void continueOpening(final Project project) {
        if (project.isDisposed()) {
            return;
        }
        registerAntListener(project);
        resetSpringGeneralSettings(project);
        fixBackOfficeJRebelSupport(project);
    }

    private boolean popupPermissionToSendStatistics(final Project project) {
        final CommonIdeaService commonIdeaService = ServiceManager.getService(CommonIdeaService.class);
        if (commonIdeaService.shouldShowPermissionToSendStatisticsDialog()) {
            EventQueue.invokeLater(() -> {
                if (new PermissionToSendStatisticsDialog(project).showAndGet()) {
                   continueOpening(project);
                }
            });
            return false;
        }
        return true;
    }

    private void resetSpringGeneralSettings(final Project project) {
        final CommonIdeaService commonIdeaService = ServiceManager.getService(CommonIdeaService.class);
        if (commonIdeaService.isHybrisProject(project)) {
            if (isPluginActive(SPRING_PLUGIN_ID)) {
                SpringGeneralSettings springGeneralSettings = SpringGeneralSettings.getInstance(project);
                springGeneralSettings.setShowMultipleContextsPanel(false);
                springGeneralSettings.setShowProfilesPanel(false);
            }
        }
    }

    private void registerAntListener(final Project project) {
        final CommonIdeaService commonIdeaService = ServiceManager.getService(CommonIdeaService.class);
        if (commonIdeaService.isHybrisProject(project)) {
            if (isPluginActive(ANT_SUPPORT_PLUGIN_ID)) {
                HybrisAntBuildListener.registerAntListener();
            }
        }
    }

    private boolean isOldHybrisProject(final Project project) {
        final CommonIdeaService commonIdeaService = ServiceManager.getService(CommonIdeaService.class);
        final StatsCollector statsCollector = ServiceManager.getService(StatsCollector.class);
        if (commonIdeaService.isHybrisProject(project)) {
            statsCollector.collectStat(StatsCollector.ACTIONS.OPEN_PROJECT);
            return commonIdeaService.isOutDatedHybrisProject(project);
        } else {
            final boolean potential = commonIdeaService.isPotentiallyHybrisProject(project);
            if (potential) {
                statsCollector.collectStat(StatsCollector.ACTIONS.OPEN_POTENTIAL_PROJECT);
            }
            return potential;
        }
    }

    private void showNotification(final Project project) {
        final NotificationGroup notificationGroup = new NotificationGroup(
            "[y] project",
            NotificationDisplayType.BALLOON,
            true,
            null,
            HybrisIcons.HYBRIS_ICON
        );

        final Notification notification = notificationGroup.createNotification(
            HybrisI18NBundleUtils.message("hybris.project.open.outdated.title"),
            HybrisI18NBundleUtils.message("hybris.project.open.outdated.text"),
            NotificationType.INFORMATION,
            (myNotification, myHyperlinkEvent) -> ProjectRefreshAction.triggerAction()
        );
        notification.setImportant(true);
        Notifications.Bus.notify(notification, project);
    }

    private void fixBackOfficeJRebelSupport(final Project project) {
        Validate.notNull(project);

        final IdeaPluginDescriptor jRebelPlugin = PluginManager.getPlugin(PluginId.getId(HybrisConstants.JREBEL_PLUGIN_ID));
        if (jRebelPlugin == null || !jRebelPlugin.isEnabled()) {
            return;
        }
        final HybrisProjectSettings hybrisProjectSettings = HybrisProjectSettingsComponent.getInstance(project).getState();
        final File compilingXml = new File(toSystemDependentName(project.getBasePath() + "/" + hybrisProjectSettings.getHybrisDirectory() +
                                           HybrisConstants.PLATFORM_MODULE_PREFIX + HybrisConstants.ANT_COMPILING_XML));
        if (!compilingXml.isFile()) {
            return;
        }
        String content;
        try {
            content = IOUtils.toString(new FileInputStream(compilingXml), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error(e);
            return;
        }
        if (!content.contains("excludes=\"**/rebel.xml\"")) {
            return;
        }
        content = content.replace("excludes=\"**/rebel.xml\"", "");
        try {
            IOUtils.write(content, new FileOutputStream(compilingXml), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error(e);
            return;
        }
    }
}
