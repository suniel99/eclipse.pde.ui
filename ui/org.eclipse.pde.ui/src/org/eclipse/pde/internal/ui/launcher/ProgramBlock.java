/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class ProgramBlock {

	protected Combo fApplicationCombo;
	private Button fProductButton;
	private Combo fProductCombo;
	private Button fApplicationButton;
	private AbstractLauncherTab fTab;
	private Listener fListener = new Listener();

	class Listener extends SelectionAdapter {		
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fProductButton) {
				boolean enabled = fProductButton.getSelection();
				fProductCombo.setEnabled(enabled);
				fApplicationCombo.setEnabled(!enabled);
			}			
			fTab.updateLaunchConfigurationDialog();
		}
	}
	
	public ProgramBlock(AbstractLauncherTab tab) {
		fTab = tab;
	}
	
	public void createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ProgramBlock_programToRun); 
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		createProductSection(group);		
		createApplicationSection(group);
	}
	
	protected void createProductSection(Composite parent) {
		fProductButton = new Button(parent, SWT.RADIO);
		fProductButton.setText(PDEUIMessages.ProgramBlock_runProduct); 
		fProductButton.addSelectionListener(fListener);
		
		fProductCombo = new Combo(parent, SWT.READ_ONLY|SWT.DROP_DOWN);
		fProductCombo.setItems(TargetPlatform.getProductNames());
		fProductCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProductCombo.addSelectionListener(fListener);
	}
	
	protected void createApplicationSection(Composite parent) {
		fApplicationButton = new Button(parent, SWT.RADIO);
		fApplicationButton.setText(PDEUIMessages.ProgramBlock_runApplication); 
			
		fApplicationCombo = new Combo(parent, SWT.READ_ONLY|SWT.DROP_DOWN);
		fApplicationCombo.setItems(getApplicationNames());
		fApplicationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fApplicationCombo.addSelectionListener(fListener);
	}
		
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		initializeProductSection(config);
		initializeApplicationSection(config);
		
		boolean useProduct = 
					config.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)
					&& PDECore.getDefault().getModelManager().isOSGiRuntime() 
					&& fProductCombo.getItemCount() > 0;
		fApplicationButton.setSelection(!useProduct);
		fApplicationCombo.setEnabled(!useProduct);
		fProductButton.setSelection(useProduct);
		fProductButton.setEnabled(fProductCombo.getItemCount() > 0);
		fProductCombo.setEnabled(useProduct);
	}
	
	protected void initializeProductSection(ILaunchConfiguration config) throws CoreException {
		if (fProductCombo.getItemCount() > 0) {
			String productName = config.getAttribute(IPDELauncherConstants.PRODUCT, (String)null);
			int index = productName == null ? -1 : fProductCombo.indexOf(productName);
			if (index == -1)
				index = 0;
			fProductCombo.setText(fProductCombo.getItem(index));
		}
	}

	protected void initializeApplicationSection(ILaunchConfiguration config) throws CoreException {
		
		String attribute = getApplicationAttribute();
		
		// first see if the application name has been set on the launch config
		String application = config.getAttribute(attribute, (String) null);
		if (application == null
			|| fApplicationCombo.indexOf(application) == -1) {
			application = null;
			
			// check if the user has entered the -application arg in the program arg field
			StringTokenizer tokenizer =
				new StringTokenizer(config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "")); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.equals("-application") && tokenizer.hasMoreTokens()) { //$NON-NLS-1$
					application = tokenizer.nextToken();
					break;
				}
			}
			
			int index = -1;
			if (application != null)
				index = fApplicationCombo.indexOf(application);
			
			// use default application as specified in the install.ini of the target platform
			if (index == -1)
				index = fApplicationCombo.indexOf(LauncherUtils.getDefaultApplicationName());
			
			if (index != -1) {
				fApplicationCombo.setText(fApplicationCombo.getItem(index));
			} else if (fApplicationCombo.getItemCount() > 0) {
				fApplicationCombo.setText(fApplicationCombo.getItem(0));
			}
		} else {
			fApplicationCombo.setText(application);
		}
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		saveApplicationSection(config);
		saveProductSection(config);
	}
	
	protected void saveProductSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.USE_PRODUCT, fProductButton.getSelection());
		config.setAttribute(IPDELauncherConstants.PRODUCT, fProductCombo.getText());
	}

	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		String text = fApplicationCombo.getText();
		String attribute = getApplicationAttribute();
		if (text.length() == 0 || text.equals(LauncherUtils.getDefaultApplicationName()))
			config.setAttribute(attribute, (String) null);
		else
			config.setAttribute(attribute, text);
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {		
		String product = TargetPlatform.getDefaultProduct();
		if (product != null) {
			config.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
			config.setAttribute(IPDELauncherConstants.PRODUCT, product); 
		}
	}
	
	protected String[] getApplicationNames() {
		return TargetPlatform.getApplicationNames();
	}
	
	protected String getApplicationAttribute() {
		return IPDELauncherConstants.APPLICATION;
	}
		
}
