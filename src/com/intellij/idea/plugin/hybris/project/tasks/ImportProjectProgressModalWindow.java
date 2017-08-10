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

package com.intellij.idea.plugin.hybris.project.tasks;

import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.framework.detection.impl.FrameworkDetectionUtil;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.idea.plugin.hybris.common.HybrisConstants;
import com.intellij.idea.plugin.hybris.common.utils.HybrisI18NBundleUtils;
import com.intellij.idea.plugin.hybris.impex.ImpexLanguage;
import com.intellij.idea.plugin.hybris.project.configurators.CompilerOutputPathsConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.ConfiguratorFactory;
import com.intellij.idea.plugin.hybris.project.configurators.ContentRootConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.FacetConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.GroupModuleConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.JavadocModuleConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.LibRootsConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.LoadedConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.ModuleSettingsConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.ModulesDependenciesConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.RunConfigurationConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.SearchScopeConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.SpringConfigurator;
import com.intellij.idea.plugin.hybris.project.configurators.VersionControlSystemConfigurator;
import com.intellij.idea.plugin.hybris.project.descriptors.CustomHybrisModuleDescriptor;
import com.intellij.idea.plugin.hybris.project.descriptors.EclipseModuleDescriptor;
import com.intellij.idea.plugin.hybris.project.descriptors.GradleModuleDescriptor;
import com.intellij.idea.plugin.hybris.project.descriptors.HybrisModuleDescriptor;
import com.intellij.idea.plugin.hybris.project.descriptors.HybrisProjectDescriptor;
import com.intellij.idea.plugin.hybris.project.descriptors.MavenModuleDescriptor;
import com.intellij.idea.plugin.hybris.project.descriptors.OotbHybrisModuleDescriptor;
import com.intellij.idea.plugin.hybris.settings.HybrisApplicationSettings;
import com.intellij.idea.plugin.hybris.settings.HybrisApplicationSettingsComponent;
import com.intellij.idea.plugin.hybris.settings.HybrisProjectSettings;
import com.intellij.idea.plugin.hybris.settings.HybrisProjectSettingsComponent;
import com.intellij.javaee.application.facet.JavaeeApplicationFacet;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.lang.Language;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.JdkVersionUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.storage.ClassPathStorageUtil;
import com.intellij.openapi.roots.impl.storage.ClasspathStorage;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.spring.facet.SpringFacet;
import com.intellij.util.PlatformUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.idea.plugin.hybris.project.descriptors.HybrisModuleDescriptor.DescriptorType.CUSTOM;
import static com.intellij.idea.plugin.hybris.project.utils.PluginCommon.JAVAEE_PLUGIN_ID;
import static com.intellij.idea.plugin.hybris.project.utils.PluginCommon.SHOW_UNLINKED_GRADLE_POPUP;
import static com.intellij.idea.plugin.hybris.project.utils.PluginCommon.SPRING_PLUGIN_ID;
import static com.intellij.idea.plugin.hybris.project.utils.PluginCommon.isPluginActive;
import static com.intellij.util.containers.ContainerUtil.newHashSet;

/**
 * Created by Martin Zdarsky-Jones on 2/11/16.
 */
public class ImportProjectProgressModalWindow extends Task.Modal {

    private final Project project;
    private final ModifiableModuleModel model;
    private final ConfiguratorFactory configuratorFactory;
    private final HybrisProjectDescriptor hybrisProjectDescriptor;
    private final boolean isUpdate;
    private final List<Module> modules;
    private ModifiableModuleModel rootProjectModifiableModel;
    private ProjectStructureConfigurable projectStructureConfigurable;
    private ModifiableRootModel modifiableRootModel;
    private ModifiableFacetModel modifiableFacetModel;

    private AccessToken token;

    public ImportProjectProgressModalWindow(
        final Project project,
        final ModifiableModuleModel model,
        final ConfiguratorFactory configuratorFactory,
        final HybrisProjectDescriptor hybrisProjectDescriptor,
        final boolean isUpdate,
        final List<Module> modules
    ) {
        super(project, HybrisI18NBundleUtils.message("hybris.project.import.commit"), false);
        this.project = project;
        this.model = model;
        this.configuratorFactory = configuratorFactory;
        this.hybrisProjectDescriptor = hybrisProjectDescriptor;
        this.isUpdate = isUpdate;
        this.modules = modules;
    }

    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.preparation"));

        final List<HybrisModuleDescriptor> allModules = hybrisProjectDescriptor
            .getModulesChosenForImport()
            .stream()
            .filter(e -> !(e instanceof MavenModuleDescriptor))
            .filter(e -> !(e instanceof EclipseModuleDescriptor))
            .filter(e -> !(e instanceof GradleModuleDescriptor))
            .collect(Collectors.toList());
        final ModifiableModelsProvider modifiableModelsProvider = configuratorFactory.getModifiableModelsProvider();
        final LibRootsConfigurator libRootsConfigurator = configuratorFactory.getLibRootsConfigurator();
        final List<FacetConfigurator> facetConfigurators = configuratorFactory.getFacetConfigurators();
        final ContentRootConfigurator regularContentRootConfigurator = configuratorFactory.getRegularContentRootConfigurator();
        final ContentRootConfigurator readOnlyContentRootConfigurator = configuratorFactory.getReadOnlyContentRootConfigurator();
        final CompilerOutputPathsConfigurator compilerOutputPathsConfigurator = configuratorFactory.getCompilerOutputPathsConfigurator();
        final ModulesDependenciesConfigurator modulesDependenciesConfigurator = configuratorFactory.getModulesDependenciesConfigurator();
        final SpringConfigurator springConfigurator = configuratorFactory.getSpringConfigurator();
        final GroupModuleConfigurator groupModuleConfigurator = configuratorFactory.getGroupModuleConfigurator();
        final LoadedConfigurator loadedConfigurator = configuratorFactory.getLoadedConfigurator();
        final JavadocModuleConfigurator javadocModuleConfigurator = configuratorFactory.getJavadocModuleConfigurator();
        final ModuleSettingsConfigurator moduleSettingsConfigurator = configuratorFactory.getModuleSettingsConfigurator();
        final VersionControlSystemConfigurator versionControlSystemConfigurator = configuratorFactory.getVersionControlSystemConfigurator();
        final RunConfigurationConfigurator debugRunConfigurationConfigurator = configuratorFactory.getDebugRunConfigurationConfigurator();
        final SearchScopeConfigurator searchScopeConfigurator = configuratorFactory.getSearchScopeConfigurator();

        this.initializeHybrisProjectSettings(project);
        this.selectSdk(project);
        this.saveCustomDirectoryLocation(project);
        this.saveImportedSettings(project);
        this.disableWrapOnType(ImpexLanguage.getInstance());
        PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false);

        if (PlatformUtils.isIdeaUltimate()) {
            indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.facets"));
            if (isPluginActive(SPRING_PLUGIN_ID)) {
                this.excludeFrameworkDetection(project, SpringFacet.FACET_TYPE_ID);
            }
            if (isPluginActive(JAVAEE_PLUGIN_ID)) {
                this.excludeFrameworkDetection(project, WebFacet.ID);
                this.excludeFrameworkDetection(project, JavaeeApplicationFacet.ID);
            }
        }

        try {
            token = ApplicationManager.getApplication().acquireReadActionLock();
            rootProjectModifiableModel = (null == model)
                ? ModuleManager.getInstance(project).getModifiableModel()
                : model;
            projectStructureConfigurable = ProjectStructureConfigurable.getInstance(
                project
            );
        } finally {
            token.finish();
        }

        indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.spring"));
        springConfigurator.findSpringConfiguration(allModules);
        groupModuleConfigurator.findDependencyModules(allModules);

        for (HybrisModuleDescriptor moduleDescriptor : allModules) {
            indicator.setText(HybrisI18NBundleUtils.message(
                "hybris.project.import.module.import",
                moduleDescriptor.getName()
            ));
            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.settings"));
            final Module javaModule = rootProjectModifiableModel.newModule(
                moduleDescriptor.getIdeaModuleFile().getAbsolutePath(), StdModuleTypes.JAVA.getId()
            );

            moduleSettingsConfigurator.configure(moduleDescriptor, javaModule);

            if (projectStructureConfigurable.isUiInitialized()) {
                final StructureConfigurableContext context = projectStructureConfigurable.getContext();
                if (null != context) {
                    context.getModulesConfigurator().getOrCreateModuleEditor(javaModule);
                }
            }

            try {
                token = ApplicationManager.getApplication().acquireReadActionLock();
                modifiableRootModel = modifiableModelsProvider.getModuleModifiableModel(
                    javaModule
                );
                modifiableFacetModel = modifiableModelsProvider.getFacetModifiableModel(
                    javaModule
                );
            } finally {
                token.finish();
            }

            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.sdk"));
            ClasspathStorage.setStorageType(modifiableRootModel, ClassPathStorageUtil.DEFAULT_STORAGE);

            ApplicationManager.getApplication().invokeAndWait(() -> WriteAction.run(
                () -> modifiableRootModel.inheritSdk()));

            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.libs"));
            libRootsConfigurator.configure(modifiableRootModel, moduleDescriptor);
            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.content"));
            if (shouldBeTreatedAsReadOnly(moduleDescriptor)) {
                readOnlyContentRootConfigurator.configure(modifiableRootModel, moduleDescriptor);
            } else {
                regularContentRootConfigurator.configure(modifiableRootModel, moduleDescriptor);
            }
            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.outputpath"));
            compilerOutputPathsConfigurator.configure(modifiableRootModel, moduleDescriptor);
            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.javadoc"));
            javadocModuleConfigurator.configure(modifiableRootModel, moduleDescriptor);
            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.groups"));
            groupModuleConfigurator.configure(rootProjectModifiableModel, javaModule, moduleDescriptor);
            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.save"));

            ApplicationManager.getApplication().invokeAndWait(() -> WriteAction.run(
                // [MG] modifiableRootModel::commit should work as well here
                () -> modifiableModelsProvider.commitModuleModifiableModel(modifiableRootModel)));

            indicator.setText2(HybrisI18NBundleUtils.message("hybris.project.import.module.facet"));
            for (FacetConfigurator facetConfigurator : facetConfigurators) {
                facetConfigurator.configure(
                    modifiableFacetModel, moduleDescriptor, javaModule, modifiableRootModel, modifiableModelsProvider
                );
            }

            modules.add(javaModule);
        }

        indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.save"));
        indicator.setText2("");
        if (!isUpdate) {
            ApplicationManager.getApplication()
                              .invokeAndWait(() -> WriteAction.run(rootProjectModifiableModel::commit));
        }

        indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.dependencies"));
        modulesDependenciesConfigurator.configure(hybrisProjectDescriptor, rootProjectModifiableModel);
        springConfigurator.configureDependencies(hybrisProjectDescriptor, rootProjectModifiableModel);
        indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.runconfigurations"));
        debugRunConfigurationConfigurator.configure(hybrisProjectDescriptor, project);
        indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.vcs"));
        versionControlSystemConfigurator.configure(project);
        indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.search.scope"));
        searchScopeConfigurator.configure(project);
        indicator.setText(HybrisI18NBundleUtils.message("hybris.project.import.finishing"));
        loadedConfigurator.configure(project, allModules);
    }

    protected void initializeHybrisProjectSettings(@NotNull final Project project) {
        Validate.notNull(project);

        final @NotNull HybrisProjectSettings hybrisProjectSettings = HybrisProjectSettingsComponent.getInstance(project)
                                                                                                   .getState();
        hybrisProjectSettings.setHybrisProject(true);
        final String version = PluginManager.getPlugin(PluginId.getId(HybrisConstants.PLUGIN_ID)).getVersion();
        hybrisProjectSettings.setImportedByVersion(version);
    }

    private void selectSdk(@NotNull final Project project) {
        Validate.notNull(project);

        final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);

        final Sdk projectSdk = projectRootManager.getProjectSdk();

        if (null == projectSdk) {
            return;
        }

        if (StringUtils.isNotBlank(projectSdk.getVersionString())) {
            final JavaSdkVersion sdkVersion = JdkVersionUtil.getVersion(projectSdk.getVersionString());
            final LanguageLevelProjectExtension languageLevelExt = LanguageLevelProjectExtension.getInstance(project);

            if (sdkVersion.getMaxLanguageLevel() != languageLevelExt.getLanguageLevel()) {
                languageLevelExt.setLanguageLevel(sdkVersion.getMaxLanguageLevel());
            }
        }
    }

    private void saveCustomDirectoryLocation(final Project project) {
        final HybrisProjectSettings hybrisProjectSettings = HybrisProjectSettingsComponent.getInstance(project)
                                                                                          .getState();
        final File customDirectory = hybrisProjectDescriptor.getExternalExtensionsDirectory();
        final File hybrisDirectory = hybrisProjectDescriptor.getHybrisDistributionDirectory();
        final File baseDirectory = VfsUtilCore.virtualToIoFile(project.getBaseDir());
        final Path projectPath = Paths.get(baseDirectory.getAbsolutePath());
        final Path hybrisPath = Paths.get(hybrisDirectory.getAbsolutePath());
        final Path relativeHybrisPath = projectPath.relativize(hybrisPath);
        hybrisProjectSettings.setHybrisDirectory(relativeHybrisPath.toString());
        if (customDirectory != null) {
            final Path customPath = Paths.get(customDirectory.getAbsolutePath());
            final Path relativeCustomPath = hybrisPath.relativize(customPath);
            hybrisProjectSettings.setCustomDirectory(relativeCustomPath.toString());
        }
    }

    private void saveImportedSettings(final Project project) {
        final HybrisProjectSettings hybrisProjectSettings = HybrisProjectSettingsComponent.getInstance(project)
                                                                                          .getState();
        final HybrisApplicationSettings appSettings = HybrisApplicationSettingsComponent.getInstance().getState();
        hybrisProjectSettings.setImportOotbModulesInReadOnlyMode(hybrisProjectDescriptor.isImportOotbModulesInReadOnlyMode());
        final File extDir = hybrisProjectDescriptor.getExternalExtensionsDirectory();
        if (extDir != null && extDir.isDirectory()) {
            hybrisProjectSettings.setExternalExtensionsDirectory(extDir.getPath());
        }
        final File configDir = hybrisProjectDescriptor.getExternalConfigDirectory();
        if (configDir != null && configDir.isDirectory()) {
            hybrisProjectSettings.setExternalConfigDirectory(configDir.getPath());
        }
        final File dbDriversDir = hybrisProjectDescriptor.getExternalDbDriversDirectory();
        if (dbDriversDir != null && dbDriversDir.isDirectory()) {
            hybrisProjectSettings.setExternalDbDriversDirectory(dbDriversDir.getPath());
            appSettings.setExternalDbDriversDirectory(dbDriversDir.getPath());
        } else {
            appSettings.setExternalDbDriversDirectory("");
        }

        hybrisProjectSettings.setCreateBackwardCyclicDependenciesForAddOns(hybrisProjectDescriptor.isCreateBackwardCyclicDependenciesForAddOn());
        final File sourceZip = hybrisProjectDescriptor.getSourceCodeZip();
        if (sourceZip != null && sourceZip.isFile()) {
            hybrisProjectSettings.setSourceCodeZip(sourceZip.getPath());
            appSettings.setSourceCodeDirectory(sourceZip.getParent());
        }
        final File modulesFilesDirectory = hybrisProjectDescriptor.getModulesFilesDirectory();
        if (modulesFilesDirectory != null && modulesFilesDirectory.isDirectory()) {
            hybrisProjectSettings.setIdeModulesFilesDirectory(modulesFilesDirectory.getPath());
        }
        final Set<String> completeSetOfHybrisModules = newHashSet();
        hybrisProjectDescriptor.getFoundModules().stream()
                               .filter(e -> e instanceof OotbHybrisModuleDescriptor || e instanceof CustomHybrisModuleDescriptor)
                               .forEach(e -> completeSetOfHybrisModules.add(e.getName()));
        hybrisProjectSettings.setCompleteSetOfAvailableExtensionsInHybris(completeSetOfHybrisModules);
    }

    private void disableWrapOnType(final Language impexLanguage) {
        final CodeStyleScheme currentScheme = CodeStyleSchemes.getInstance().getCurrentScheme();
        final CodeStyleSettings codeStyleSettings = currentScheme.getCodeStyleSettings();
        if (impexLanguage != null) {
            CommonCodeStyleSettings langSettings = codeStyleSettings.getCommonSettings(impexLanguage);
            if (langSettings != null) {
                langSettings.WRAP_ON_TYPING = CommonCodeStyleSettings.WrapOnTyping.NO_WRAP.intValue;
            }
        }
    }

    private void excludeFrameworkDetection(final Project project, FacetTypeId facetTypeId) {
        final DetectionExcludesConfiguration configuration = DetectionExcludesConfiguration.getInstance(project);
        final FacetType facetType = FacetTypeRegistry.getInstance().findFacetType(facetTypeId);
        if (facetType != null) {
            final FrameworkType frameworkType = FrameworkDetectionUtil.findFrameworkTypeForFacetDetector(facetType);
            if (frameworkType != null) {
                configuration.addExcludedFramework(frameworkType);
            }
        }
    }

    private boolean shouldBeTreatedAsReadOnly(final HybrisModuleDescriptor moduleDescriptor) {
        if (!moduleDescriptor.hasServerJar() || moduleDescriptor.getName().equals("hmc")) {
            return false;
        }
        if (moduleDescriptor.getDescriptorType() == CUSTOM) {
            return false;
        }
        if (moduleDescriptor.getRootProjectDescriptor().isImportOotbModulesInReadOnlyMode()) {
            return true;
        }
        return false;
    }
}
