/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.pde.internal.ui.PDEUIMessages;

import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.shared.target.BundleContainerTable;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.*;

/**
 * Section for editing the content of the target (bundle containers) in the target definition editor
 * @see DefinitionPage
 * @see TargetEditor
 */
public class ContentSection extends SectionPart {

	private BundleContainerTable fTable;
	private TargetEditor fEditor;

	public ContentSection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fEditor = (TargetEditor) page.getEditor();
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/**
	 * @return The target model backing this editor
	 */
	private ITargetDefinition getTarget() {
		return fEditor.getTarget();
	}

	/**
	 * Creates the UI for this section.
	 * 
	 * @param section section the UI is being added to
	 * @param toolkit form toolkit used to create the widgets
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		sectionData.horizontalSpan = 2;
		section.setLayoutData(sectionData);
		section.setText(PDEUIMessages.ContentSection_0);
		// TODO Delete NL'd messages that are no longer relevent, such as the content/environment description
//		section.setDescription(PDEUIMessages.OverviewPage_contentDescription);

		section.setDescription(PDEUIMessages.ContentSection_1);
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		client.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

//		Composite tableContainer = toolkit.createComposite(client);
//		tableContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		tableContainer.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		fTable = BundleContainerTable.createTableInForm(client, toolkit, this);
		fTable.setInput(getTarget());

		toolkit.paintBordersFor(client);

		section.setClient(client);
	}

//	/* (non-Javadoc)
//	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
//	 */
//	public void commit(boolean onSave) {
//		// TODO Commit the table? May not be necessary
//		fTable.commit(onSave);
//	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fTable.refresh();
		super.refresh();
	}

}
