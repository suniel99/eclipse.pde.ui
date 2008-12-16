/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * This class is used to exclude some deltas from the generated report.
 */
public class ExcludeListDeltaVisitor extends DeltaXmlVisitor {
	private Set excludedElement;
	private String excludeListLocation;
	private List nonExcludedElements;

	public ExcludeListDeltaVisitor(String excludeListLocation) throws CoreException {
		super();
		this.excludeListLocation = excludeListLocation;
		this.nonExcludedElements = new ArrayList();
	}
	private boolean checkExclude(IDelta delta) {
		if (this.excludedElement == null) {
			this.excludedElement = CommonUtilsTask.initializeExcludedElement(this.excludeListLocation);
		}
		return isExcluded(delta);
	}
	public String getPotentialExcludeList() {
		if (this.nonExcludedElements == null) return Util.EMPTY_STRING;
		Collections.sort(this.nonExcludedElements);
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		for (Iterator iterator = this.nonExcludedElements.iterator(); iterator.hasNext(); ) {
			writer.println(iterator.next());
		}
		writer.close();
		return String.valueOf(stringWriter.getBuffer());
	}
	private boolean isExcluded(IDelta delta) {
		String typeName = delta.getTypeName();
		StringBuffer buffer = new StringBuffer();
		String componentId = delta.getApiComponentID();
		if (componentId != null) {
			buffer.append(componentId).append(':');
		}
		if (typeName != null) {
			buffer.append(typeName);
		}
		int flags = delta.getFlags();
		switch(flags) {
			case IDelta.TYPE_MEMBER :
				buffer.append('.').append(delta.getKey());
				break;
			case IDelta.METHOD :
			case IDelta.CONSTRUCTOR :
			case IDelta.ENUM_CONSTANT :
			case IDelta.METHOD_WITH_DEFAULT_VALUE :
			case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
			case IDelta.FIELD :
				buffer.append('#').append(delta.getKey());
				break;
			case IDelta.MAJOR_VERSION :
			case IDelta.MINOR_VERSION :
				buffer
					.append(Util.getDeltaFlagsName(flags))
					.append('_')
					.append(Util.getDeltaKindName(delta.getKind()));
				break;
		}
		String excludeListKey = String.valueOf(buffer);
		if (this.excludedElement.contains(excludeListKey)) {
			return true;
		}
		this.nonExcludedElements.add(excludeListKey);
		return false;
	}
	protected void processLeafDelta(IDelta delta) {
		if (DeltaProcessor.isCompatible(delta)) {
			switch(delta.getKind()) {
				case IDelta.ADDED :
					int modifiers = delta.getModifiers();
					if (Util.isPublic(modifiers)) {
						// if public, we always want to check @since tags
						switch(delta.getFlags()) {
							case IDelta.TYPE_MEMBER :
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
							case IDelta.ENUM_CONSTANT :
							case IDelta.METHOD_WITH_DEFAULT_VALUE :
							case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
							case IDelta.FIELD :
							case IDelta.TYPE :
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
								break;
						}
					} else if (Util.isProtected(modifiers) && !RestrictionModifiers.isExtendRestriction(delta.getRestrictions())) {
						// if protected, we only want to check @since tags if the enclosing class can be subclassed
						switch(delta.getFlags()) {
							case IDelta.TYPE_MEMBER :
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
							case IDelta.ENUM_CONSTANT :
							case IDelta.FIELD :
							case IDelta.TYPE :
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
								break;
						}
					}
					break;
				case IDelta.CHANGED :
					switch(delta.getFlags()) {
						case IDelta.MAJOR_VERSION :
						case IDelta.MINOR_VERSION :
							if (!checkExclude(delta)) {
								super.processLeafDelta(delta);
							}
					}
			}
		} else {
			if (delta.getKind() == IDelta.ADDED) {
				// if public, we always want to check @since tags
				switch(delta.getFlags()) {
					case IDelta.TYPE_MEMBER :
					case IDelta.METHOD :
					case IDelta.CONSTRUCTOR :
					case IDelta.ENUM_CONSTANT :
					case IDelta.METHOD_WITH_DEFAULT_VALUE :
					case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
					case IDelta.FIELD :
						// ensure that there is a @since tag for the corresponding member
						if (delta.getKind() == IDelta.ADDED && Util.isVisible(delta)) {
							if (!checkExclude(delta)) {
								super.processLeafDelta(delta);
							}
						}
				}
			}
		}
	}
}