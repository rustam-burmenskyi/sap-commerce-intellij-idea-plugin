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

package com.intellij.idea.plugin.hybris.reference.provider

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.spring.SpringManager
import com.intellij.spring.model.CommonSpringBean
import com.intellij.spring.model.SpringBeanPointer
import com.intellij.spring.model.utils.SpringModelSearchers
import com.intellij.util.ProcessingContext
import org.apache.commons.lang3.StringUtils

class HybrisSpringProcessReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {

        val reference = object : PsiReferenceBase<PsiElement>(element, true), PsiPolyVariantReference {

            private val QUOTE_LENGTH = 2

            override fun getRangeInElement() = TextRange.from(1, element.textLength - QUOTE_LENGTH)

            override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
                val project = myElement.project
                val name = myElement.text.replace("[\"\']".toRegex(), StringUtils.EMPTY)

                val module = ModuleUtil.findModuleForPsiElement(element) ?: return ResolveResult.EMPTY_ARRAY

                return SpringManager.getInstance(project).getAllModels(module)
                    .mapNotNull { SpringModelSearchers.findBean(it, name) }
                    .map { it.beanClass }
                    .toCollection(mutableSetOf())
                    .let { createResults(it) }
            }

            override fun resolve(): PsiElement? {
                val resolveResults = multiResolve(false)
                return if (resolveResults.size == 1) resolveResults[0].element else null
            }

            override fun getVariants() = PsiReference.EMPTY_ARRAY
        }

        return arrayOf(reference)
    }
}

    