/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@gentleware.com> - bug 194529
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.editor.contentassist.XMLElementProposalComputer;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.wizards.extension.ExtensionEditorWizard;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionWizard;
import org.eclipse.pde.ui.IExtensionEditorWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ExtensionsSection extends TreeSection implements IModelChangedListener, IPropertyChangeListener {
	private TreeViewer fExtensionTree;
	private Image fExtensionImage;
	private Image fGenericElementImage;
	private FormFilteredTree fFilteredTree;
	private SchemaRegistry fSchemaRegistry;
	private Hashtable fEditorWizards;
	private SortAction fSortAction;
	private CollapseAction fCollapseAction;

	private static final String[] COMMON_LABEL_PROPERTIES = {
		"label", //$NON-NLS-1$
		"name", //$NON-NLS-1$
		"id"}; //$NON-NLS-1$

	class ExtensionContentProvider extends DefaultContentProvider
	implements
	ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			Object[] children = null;
			if (parent instanceof IPluginBase)
				children = ((IPluginBase) parent).getExtensions();
			else if (parent instanceof IPluginExtension) {
				children = ((IPluginExtension) parent).getChildren();
			} else if (parent instanceof IPluginElement) {
				children = ((IPluginElement) parent).getChildren();
			}
			if (children == null)
				children = new Object[0];
			return children;
		}
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginExtension) {
				return ((IPluginModelBase)getPage().getModel()).getPluginBase();
			}
			if (child instanceof IPluginObject)
				return ((IPluginObject) child).getParent();
			return null;
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
	}
	class ExtensionLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return resolveObjectName(obj);
		}
		public Image getImage(Object obj) {
			return resolveObjectImage(obj);
		}
	}
	public ExtensionsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[]{
				PDEUIMessages.ManifestEditor_DetailExtension_new, 
				PDEUIMessages.ManifestEditor_DetailExtension_edit,
				PDEUIMessages.ManifestEditor_DetailExtension_up,
				PDEUIMessages.ManifestEditor_DetailExtension_down});
		fHandleDefaultButton = false;
	}
	private static void addItemsForExtensionWithSchema(MenuManager menu,
			IPluginExtension extension, IPluginParent parent) {
		ISchema schema = getSchema(extension);
		String tagName = (parent == extension ? "extension" : parent.getName()); //$NON-NLS-1$
		ISchemaElement elementInfo = schema.findElement(tagName);
		
		if ((elementInfo != null) &&
				(elementInfo.getType() instanceof ISchemaComplexType) &&
				(parent instanceof IDocumentNode)) {
			// We have a schema complex type.  Either the element has attributes
			// or the element has children.
			// Generate the list of element proposals
			TreeSet elementSet = XMLElementProposalComputer
					.computeElementProposal(elementInfo, (IDocumentNode)parent);
			
			// Create a corresponding menu entry for each element proposal
			Iterator iterator = elementSet.iterator();
			while (iterator.hasNext()) {
				Action action = new NewElementAction((ISchemaElement) iterator
						.next(), parent);
				menu.add(action);	
			}			
		}
	}

	/**
	 * @param parent
	 * @return
	 */
	private static ISchema getSchema(IPluginParent parent) {
		if (parent instanceof IPluginExtension) {
			return getSchema((IPluginExtension)parent);
		} else if (parent instanceof IPluginElement) {
			return getSchema((IPluginElement)parent);
		} else {
			return null;
		}
	}
	
	private static ISchema getSchema(IPluginExtension extension) {
		String point = extension.getPoint();
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		return registry.getSchema(point);
	}
	
	/**
	 * @param element
	 * @return
	 */
	static ISchemaElement getSchemaElement(IPluginElement element) {
		ISchema schema = getSchema(element);
		if (schema != null) {
			return schema.findElement(element.getName());
		}
		return null;
	}
	
	/**
	 * @param element
	 * @return
	 */
	private static ISchema getSchema(IPluginElement element) {
		IPluginObject parent = element.getParent();
		while (parent != null && !(parent instanceof IPluginExtension)) {
			parent = parent.getParent();
		}
		if (parent != null) {
			return getSchema((IPluginExtension) parent);
		}
		return null;		
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		initializeImages();
		Composite container = createClientContainer(section, 2, toolkit);
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		fExtensionTree = treePart.getTreeViewer();
		fExtensionTree.setContentProvider(new ExtensionContentProvider());
		fExtensionTree.setLabelProvider(new ExtensionLabelProvider());
		toolkit.paintBordersFor(container);
		section.setClient(container);
		section.setDescription(PDEUIMessages.ExtensionsSection_sectionDescExtensionsMaster);
		// See Bug # 160554: Set text before text client
		section.setText(PDEUIMessages.ManifestEditor_DetailExtension_title);
		initialize((IPluginModelBase) getPage().getModel());
		createSectionToolbar(section, toolkit);
		// Create the adapted listener for the filter entry field
		fFilteredTree.createUIListenerEntryFilter(this);
		fFilteredTree.getFilterControl().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				StructuredViewer viewer = getStructuredViewerPart().getViewer();
				IStructuredSelection ssel = (IStructuredSelection)viewer.getSelection();
				updateButtons(ssel.size() != 1 ? null : ssel.getFirstElement());
			}
		});
	}
	
	/**
	 * @param section
	 * @param toolkit
	 */
	private void createSectionToolbar(Section section, FormToolkit toolkit) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) &&
						(handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});			
		// Add sort action to the tool bar
		fSortAction = new SortAction(fExtensionTree, 
				PDEUIMessages.ExtensionsPage_sortAlpha, null, null, this);
		toolBarManager.add(fSortAction);
		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fExtensionTree, 
				PDEUIMessages.ExtensionsPage_collapseAll);
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons(selection.getFirstElement());
		getTreePart().getButton(1).setVisible(isSelectionEditable(selection));
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
		/*
		 * PropertiesAction action = new
		 * PropertiesAction(getFormPage().getEditor()); action.run();
		 */
	}
	protected void buttonSelected(int index) {
		switch (index) {
		case 0 :
			handleNew();
			break;
		case 1 :
			handleEdit();
			break;
		case 2 :
			handleMove(true);
			break;
		case 3 :
			handleMove(false);
			break;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// Explicitly call the dispose method on the extensions tree
		if (fFilteredTree != null) {
			fFilteredTree.dispose();
		}
		fEditorWizards = null;
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
		.getAggregateModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		
		if (!isEditable()) { return false; }
		
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		
		return false;
	}
	
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginExtension
				|| object instanceof IPluginElement) {
			fExtensionTree.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fExtensionTree.getSelection();
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			Object object = ssel.getFirstElement();
			if (object instanceof IPluginParent) {
				IPluginParent parent = (IPluginParent) object;
				if (parent.getModel().getUnderlyingResource() != null) {
					fillContextMenu(getPage(), parent, manager);
					manager.add(new Separator());
				}
			}
		} else if (ssel.size() > 1) {
			// multiple
			Action delAction = new Action() {
				public void run() {
					handleDelete();
				}
			};
			delAction.setText(PDEUIMessages.Actions_delete_label);
			manager.add(delAction);
			manager.add(new Separator());
			delAction.setEnabled(isEditable());
		}
		if (ssel.size() == 1) {
			manager.add(new Separator());
			Object object = ssel.getFirstElement();
			if (object instanceof IPluginExtension) {
				PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				manager.add(new Separator());
			}
			//manager.add(new PropertiesAction(getFormPage().getEditor()));
		}
		manager.add(new Separator());
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().addClipboardActions(manager);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager, false);

	}
	static IMenuManager fillContextMenu(PDEFormPage page,
			final IPluginParent parent, IMenuManager manager) {
		return fillContextMenu(page, parent, manager, false);
	}
	static IMenuManager fillContextMenu(PDEFormPage page,
			final IPluginParent parent, IMenuManager manager,
			boolean addSiblingItems) {
		return fillContextMenu(page, parent, manager, addSiblingItems, true);
	}
	static IMenuManager fillContextMenu(PDEFormPage page,
			final IPluginParent parent, IMenuManager manager,
			boolean addSiblingItems, boolean fullMenu) {
		MenuManager menu = new MenuManager(PDEUIMessages.Menus_new_label);
		IPluginExtension extension = getExtension(parent);
		ISchema schema = getSchema(extension);
		if (schema == null) {
			menu.add(new NewElementAction(null, parent));
		} else {
			addItemsForExtensionWithSchema(menu, extension, parent);
			if (addSiblingItems) {
				IPluginObject parentsParent = parent.getParent();
				if (!(parentsParent instanceof IPluginExtension)) {
					IPluginParent pparent = (IPluginParent) parentsParent;
					menu.add(new Separator());
					addItemsForExtensionWithSchema(menu, extension, pparent);
				}
			}
		}
		manager.add(menu);
		manager.add(new Separator());
		if (fullMenu) {
			Action deleteAction = new Action(PDEUIMessages.Actions_delete_label) {
				public void run() {
					try {
						IPluginObject parentsParent = parent.getParent();
						if (parent instanceof IPluginExtension) {
							IPluginBase plugin = (IPluginBase) parentsParent;
							plugin.remove((IPluginExtension) parent);
						} else {
							IPluginParent parentElement = (IPluginParent) parent
							.getParent();
							parentElement.remove(parent);
						}
					} catch (CoreException e) {
					}
				}
			};
			deleteAction.setEnabled(page.getModel().isEditable());
			manager.add(deleteAction);
		}
		return menu;
	}
	static IPluginExtension getExtension(IPluginParent parent) {
		while (parent != null && !(parent instanceof IPluginExtension)) {
			parent = (IPluginParent) parent.getParent();
		}
		return (IPluginExtension) parent;
	}
	private void handleDelete() {
		IStructuredSelection sel = (IStructuredSelection) fExtensionTree
		.getSelection();
		if (sel.isEmpty())
			return;
		for (Iterator iter = sel.iterator(); iter.hasNext();) {
			IPluginObject object = (IPluginObject) iter.next();
			try {
				if (object instanceof IPluginElement) {
					IPluginElement ee = (IPluginElement) object;
					IPluginParent parent = (IPluginParent) ee.getParent();
					parent.remove(ee);
				} else if (object instanceof IPluginExtension) {
					IPluginExtension extension = (IPluginExtension) object;
					IPluginBase plugin = extension.getPluginBase();
					plugin.remove(extension);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void handleNew() {
		final IProject project = getPage().getPDEEditor().getCommonProject();
		BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(),
				new Runnable() {
			public void run() {
				((ManifestEditor)getPage().getEditor()).ensurePluginContextPresence();
				NewExtensionWizard wizard = new NewExtensionWizard(
						project, (IPluginModelBase) getPage()
						.getModel(), (ManifestEditor) getPage()
						.getPDEEditor()) {
					public boolean performFinish() {
						return super.performFinish();
					}
				};
				WizardDialog dialog = new WizardDialog(PDEPlugin
						.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 500, 500);
				dialog.open();
			}
		});
	}
	private void handleEdit(IConfigurationElement element, IStructuredSelection selection) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		try {
			final IExtensionEditorWizard wizard = (IExtensionEditorWizard)element.createExecutableExtension("class"); //$NON-NLS-1$
			wizard.init(project, model, selection);
			BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(),
					new Runnable() {
				public void run() {
					WizardDialog dialog = new WizardDialog(PDEPlugin
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 500, 500);
					dialog.open();
				}
			});
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void handleEdit() {
		final IStructuredSelection selection = (IStructuredSelection)fExtensionTree.getSelection();
		ArrayList editorWizards = getEditorWizards(selection);
		if (editorWizards==null) return;
		if (editorWizards.size()==1) {
			// open the wizard directly			
			handleEdit((IConfigurationElement)editorWizards.get(0), selection);
		}
		else {
			IProject project = getPage().getPDEEditor().getCommonProject();
			IPluginModelBase model = (IPluginModelBase)getPage().getModel();
			final ExtensionEditorWizard wizard = new ExtensionEditorWizard(project, model, selection);
			BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(),
					new Runnable() {
				public void run() {
					WizardDialog dialog = new WizardDialog(PDEPlugin
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 500, 500);
					dialog.open();
				}
			});
		}
	}
	private ArrayList getEditorWizards(IStructuredSelection selection) {
		if (selection.size()!=1) return null;
		Object obj = selection.getFirstElement();
		String pointId = null;
		if (obj instanceof IPluginExtension) {
			pointId = ((IPluginExtension)obj).getPoint();
		}
		else if (obj instanceof IPluginElement) {
			IPluginObject parent = ((IPluginElement)obj).getParent();
			while (parent!=null) {
				if (parent instanceof IPluginExtension) {
					pointId = ((IPluginExtension)parent).getPoint();
					break;
				}
				parent = parent.getParent();
			}
		}
		if (pointId==null) return null;
		if (fEditorWizards==null)
			loadExtensionWizards();
		return (ArrayList)fEditorWizards.get(pointId);
	}

	private void loadExtensionWizards() {
		fEditorWizards = new Hashtable();
		IConfigurationElement [] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.pde.ui.newExtension"); //$NON-NLS-1$
		for (int i=0; i<elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("editorWizard")) { //$NON-NLS-1$
				String pointId = element.getAttribute("point"); //$NON-NLS-1$
				if (pointId==null) continue;
				ArrayList list = (ArrayList)fEditorWizards.get(pointId);
				if (list==null) {
					list = new ArrayList();
					fEditorWizards.put(pointId, list);
				}
				list.add(element);
			}
		}
	}
	private boolean isSelectionEditable(IStructuredSelection selection) {
		if (!getPage().getModel().isEditable())
			return false;
		return getEditorWizards(selection)!=null;
	}

	public void initialize(IPluginModelBase model) {
		fExtensionTree.setInput(model.getPluginBase());
		selectFirstExtension();
		boolean editable = model.isEditable();
		TreePart treePart = getTreePart();
		treePart.setButtonEnabled(0, editable);
		treePart.setButtonEnabled(1, false);
		treePart.setButtonEnabled(3, false);
		treePart.setButtonEnabled(4, false);
		model.addModelChangedListener(this);
	}
	private void selectFirstExtension() {
		Tree tree = fExtensionTree.getTree();
		TreeItem [] items = tree.getItems();
		if (items.length==0) return;
		TreeItem firstItem = items[0];
		Object obj = firstItem.getData();
		fExtensionTree.setSelection(new StructuredSelection(obj));
	}
	void fireSelection() {
		fExtensionTree.setSelection(fExtensionTree.getSelection());
	}
	public void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		fExtensionImage = provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
		fGenericElementImage = provider
		.get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	}
	public void refresh() {
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		fExtensionTree.setInput(model.getPluginBase());
		selectFirstExtension();
		getManagedForm().fireSelectionChanged(ExtensionsSection.this,
				fExtensionTree.getSelection());
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginBase
				&& event.getChangeType() == IModelChangedEvent.CHANGE
				&& event.getChangedProperty().equals(
						IExtensions.P_EXTENSION_ORDER)) {
			IStructuredSelection sel = (IStructuredSelection) fExtensionTree
			.getSelection();
			IPluginExtension extension = (IPluginExtension) sel
			.getFirstElement();
			fExtensionTree.refresh();
			fExtensionTree.setSelection(new StructuredSelection(extension));
			return;
		}
		if (changeObject instanceof IPluginExtension
				|| (changeObject instanceof IPluginElement && ((IPluginElement)changeObject).getParent() instanceof IPluginParent)) {
			IPluginObject pobj = (IPluginObject) changeObject;
			IPluginObject parent = changeObject instanceof IPluginExtension
			? ((IPluginModelBase) getPage().getModel()).getPluginBase()
					: pobj.getParent();
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				fExtensionTree.add(parent, pobj);
				fExtensionTree.setSelection(
						new StructuredSelection(changeObject), true);
				fExtensionTree.getTree().setFocus();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				fExtensionTree.remove(pobj);
			} else {
				if (event.getChangedProperty().equals(
						IPluginParent.P_SIBLING_ORDER)) {
					IStructuredSelection sel = (IStructuredSelection) fExtensionTree
					.getSelection();
					IPluginObject child = (IPluginObject) sel.getFirstElement();
					fExtensionTree.refresh(child.getParent());
					fExtensionTree.setSelection(new StructuredSelection(child));
				} else {
					fExtensionTree.update(changeObject, null);
				}
			}
		}
	}

	private Image resolveObjectImage(Object obj) {
		if (obj instanceof IPluginExtension) {
			return fExtensionImage;
		}
		Image elementImage = fGenericElementImage;
		if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			Image customImage = getCustomImage(element);
			if (customImage != null)
				elementImage = customImage;
			String bodyText = element.getText();
			boolean hasBodyText = bodyText!=null&&bodyText.length()>0;
			if (hasBodyText) {
				elementImage = PDEPlugin.getDefault().getLabelProvider().get(
						elementImage, SharedLabelProvider.F_EDIT);
			}
		}
		return elementImage;
	}

	private static boolean isStorageModel(IPluginObject object) {
		IPluginModelBase modelBase = object.getPluginModel();
		return modelBase.getInstallLocation()==null;
	}

	static Image getCustomImage(IPluginElement element) {
		if (isStorageModel(element))return null;
		ISchemaElement elementInfo = getSchemaElement(element);
		if (elementInfo != null && elementInfo.getIconProperty() != null) {
			String iconProperty = elementInfo.getIconProperty();
			IPluginAttribute att = element.getAttribute(iconProperty);
			String iconPath = null;
			if (att != null && att.getValue() != null) {
				iconPath = att.getValue();
			}
			if (iconPath != null) {
				//OK, we have an icon path relative to the plug-in
				return getImageFromPlugin(element, iconPath);
			}
		}
		return null;
	}

	private static Image getImageFromPlugin(IPluginElement element,
			String iconPathName) {
		// 39283 - ignore icon paths that
		// point at plugin.properties
		if (iconPathName.startsWith("%")) //$NON-NLS-1$
			return null;

		IPluginModelBase model = element.getPluginModel();
		if (model == null)
			return null;

		return PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(model, iconPathName);
	}
	private String resolveObjectName(Object obj) {
		return resolveObjectName(getSchemaRegistry(), obj);
	}

	private SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = PDECore.getDefault().getSchemaRegistry();
		return fSchemaRegistry;
	}

	public static String resolveObjectName(SchemaRegistry schemaRegistry, Object obj) {
		boolean fullNames = PDEPlugin.isFullNameModeEnabled();
		if (obj instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) obj;
			if (!fullNames) {
				return extension.getPoint();
			}
			if (extension.getName() != null)
				return extension.getTranslatedName();
			ISchema schema = schemaRegistry.getSchema(extension.getPoint());
			// try extension point schema definition
			if (schema != null) {
				// exists
				return schema.getName();
			}
			return extension.getPoint();		
		} else if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			String baseName = element.getName();			
			String fullName = null;
			ISchemaElement elementInfo = getSchemaElement(element);
			IPluginAttribute labelAtt = null;
			if (elementInfo != null && elementInfo.getLabelProperty() != null) {
				labelAtt = element.getAttribute(elementInfo.getLabelProperty());
			}
			if (labelAtt == null) {
				// try some hard-coded attributes that
				// are used frequently
				for (int i = 0; i < COMMON_LABEL_PROPERTIES.length; i++) {
					labelAtt = element.getAttribute(COMMON_LABEL_PROPERTIES[i]);
					if (labelAtt != null)
						break;
				}
				if (labelAtt == null) {
					// Last try - if there is only one attribute,
					// use that
					if (element.getAttributeCount() == 1)
						labelAtt = element.getAttributes()[0];
				}
			}
			if (labelAtt != null && labelAtt.getValue() != null)
				fullName = stripShortcuts(labelAtt.getValue());
			fullName = element.getResourceString(fullName);
			if (fullNames)
				return fullName != null ? fullName : baseName;
			return fullName != null
			? (fullName + " (" + baseName + ")") //$NON-NLS-1$ //$NON-NLS-2$
					: baseName;
		}
		return obj.toString();
	}
	public void setFocus() {
		if (fExtensionTree != null)
			fExtensionTree.getTree().setFocus();
	}
	public static String stripShortcuts(String input) {
		StringBuffer output = new StringBuffer();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '&')
				continue;
			else if (c == '@')
				break;
			output.append(c);
		}
		return output.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		// Note:  Multi-select in tree viewer is disabled; but, this function
		// can support multiple source objects
		// Rule:  Element source objects are always pasted as children of the
		// target object (if allowable)
		// Rule:  Extension source objects are always pasted and are independent
		// of the target object
		// Ensure all the sourceObjects are either extensions or elements
		boolean allExtensions = true;
		boolean allElements = true;
		for (int i = 0; i < sourceObjects.length; i++) {
			if (sourceObjects[i] instanceof IPluginExtension) {
				allElements = false;
			} else if (sourceObjects[i] instanceof IPluginElement) {
				allExtensions = false;
			} else {
				return false;
			}
		}
		// Because of the extension rule, we can paste all extension source
		// objects
		if (allExtensions) {
			return true;
		}
		// Pasting a mixture of elements and extensions is not supported
		// (or wise from the users perspective)
		if (allElements == false) {
			return false;
		}
		// Ensure the target object can have children 
		if ((targetObject instanceof IPluginParent) == false) {
			return false;
		} else if ((targetObject instanceof IDocumentNode) == false) {
			return false;
		}
		// Retrieve the schema corresponding to the target object		
		IPluginParent targetParent = (IPluginParent)targetObject;
		ISchema schema = getSchema(targetParent);
		// If there is no schema, then a source object can be pasted as a 
		// child of any target object
		if (schema == null) {
			return true;
		}
		// Determine the element name of the target object
		// getName() does not work for extensions for some reason
		String tagName = null;
		if (targetParent instanceof IPluginExtension) {
			tagName = "extension"; //$NON-NLS-1$
		} else {
			tagName = targetParent.getName();
		}
		// Retrieve the element schema for the target object
		ISchemaElement schemaElement = schema.findElement(tagName);
		// Ensure we found a schema element and it is a schema complex type
		if (schemaElement == null) {
			// Something is seriously wrong, we have a schema
			return false;
		} else if ((schemaElement.getType() instanceof ISchemaComplexType) == false) {
			// Something is seriously wrong, we are a plugin parent
			return false;
		}
		// We have a schema complex type.  Either the target object has 
		// attributes or the element has children.
		// Generate the list of element proposals
		TreeSet elementSet = 
			XMLElementProposalComputer.computeElementProposal(
					schemaElement, (IDocumentNode)targetObject);
		// Determine whether we can paste the source elements as children of
		// the target object
		if (sourceObjects.length > 1) {
			return canPasteSourceElements((IPluginElement[])sourceObjects, elementSet);
		}
		return canPasteSourceElement((IPluginElement)sourceObjects[0], elementSet);
	}

	/**
	 * @param sourceElements
	 * @param targetElementSet
	 * @return
	 */
	private boolean canPasteSourceElements(IPluginElement[] sourceElements,
			TreeSet targetElementSet) {
		// Performance optimization
		// HashSet of schema elements is not comparable for the source
		// objects (schema elements are transient)
		// Create a new HashSet with element names for comparison		
		HashSet targetElementNameSet = new HashSet();
		Iterator iterator = targetElementSet.iterator();
		while (iterator.hasNext()) {
			targetElementNameSet.add(((ISchemaElement)iterator.next()).getName());
		}
		// Paste will be enabled only if all source objects can be pasted 
		// as children into the target element
		// Limitation:  Multiplicity checks will be compromised because we
		// are pasting multiple elements as a single transaction.  The 
		// mulitplicity check is computed on the current static state of the
		// target object with the assumption one new element will be added.
		// Obviously, adding more than one element can invalidate the check
		// due to choice, sequence multiplicity constraints.  Even if source
		// elements that are pasted violate multiplicity constraints the 
		// extensions builder will flag them with errors
		for (int i = 0; i < sourceElements.length; i++) {
			String sourceTagName = sourceElements[i].getName();
			if (targetElementNameSet.contains(sourceTagName) == false) {
				return false;
			}
		}		
		return true;
	}
	
	/**
	 * @param sourceElement
	 * @param targetElementSet
	 * @return
	 */
	private boolean canPasteSourceElement(IPluginElement sourceElement, 
			TreeSet targetElementSet) {
		boolean canPaste = false;
		// Get the source element tag name
		String sourceTagName = sourceElement.getName();
		// Iterate over set of valid element proposals
		Iterator iterator = targetElementSet.iterator();
		while (iterator.hasNext()) {
			// Get the proposal element tag name
			String targetTagName = ((ISchemaElement)iterator.next()).getName();
			// Only a source element that is found ithin the set of element 
			// proposals can be pasted
			if (sourceTagName.equals(targetTagName)) {
				canPaste = true;
				break;
			}
		}			
		return canPaste;
	}
	
	/**
	 * @return
	 */
	private IPluginModelBase getPluginModelBase() {
		// Note:  This method will work with fragments as long as a fragment.xml
		// is defined first.  Otherwise, paste will not work out of the box.
		// Get the model
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		// Ensure the model is a bundle plugin model
		if ((model instanceof IBundlePluginModelBase) == false) {
			return null;
		}
		// Get the extension model
		ISharedExtensionsModel extensionModel = 
			((IBundlePluginModelBase)model).getExtensionsModel();
		// Ensure the extension model is defined
		if ((extensionModel == null) || 
				((extensionModel instanceof IPluginModelBase) == false)) {
			return null;
		}
		return ((IPluginModelBase)extensionModel);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// By default, fragment.xml does not exist until the first extension
		// or extension point is created.  
		// Ensure the file exists before pasting because the model will be 
		// null and the paste will fail if it does not exist
		((ManifestEditor)getPage().getEditor()).ensurePluginContextPresence();
		// Note:  Multi-select in tree viewer is disabled; but, this function
		// can support multiple source objects
		// Get the model
		IPluginModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		IPluginBase pluginBase = model.getPluginBase();
		try {
			// Paste all source objects into the target object
			for (int i = 0; i < sourceObjects.length; i++) {
				Object sourceObject = sourceObjects[i];
				
				if ((sourceObject instanceof IPluginExtension) &&
						(pluginBase instanceof IDocumentNode)) {
					// Extension object
					IDocumentNode extension = (IDocumentNode)sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					extension.reconnect((IDocumentNode)pluginBase, model);
					// Add the extension to the plugin parent (plugin)
					pluginBase.add((IPluginExtension)extension);

				} else if ((sourceObject instanceof IPluginElement) &&
						(targetObject instanceof IPluginParent) &&
						(targetObject instanceof IDocumentNode)) {
					// Element object
					IDocumentNode element = (IDocumentNode)sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					element.reconnect((IDocumentNode)targetObject, model);
					// Add the element to the plugin parent (extension or
					// element)
					((IPluginParent)targetObject).add((IPluginElement)element);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	private void handleMove(boolean up) {
		IStructuredSelection sel = (IStructuredSelection) fExtensionTree
		.getSelection();
		IPluginObject object = (IPluginObject) sel.getFirstElement();
		if (object instanceof IPluginElement) {
			IPluginParent parent = (IPluginParent) object.getParent();
			IPluginObject[] children = parent.getChildren();
			int index = parent.getIndexOf(object);
			int newIndex = up ? index - 1 : index + 1;
			IPluginObject child2 = children[newIndex];
			try {
				parent.swap(object, child2);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		} else if (object instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) object;
			IPluginBase plugin = extension.getPluginBase();
			IPluginExtension[] extensions = plugin.getExtensions();
			int index = plugin.getIndexOf(extension);
			int newIndex = up ? index - 1 : index + 1;
			IPluginExtension e2 = extensions[newIndex];
			try {
				plugin.swap(extension, e2);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	
	private void updateButtons(Object item) {
		if (getPage().getModel().isEditable() == false)
			return;
		boolean sorted = fSortAction != null && fSortAction.isChecked();
		if (sorted) {
			getTreePart().setButtonEnabled(2, false);
			getTreePart().setButtonEnabled(3, false);
			return;
		}
		
		boolean filtered = fFilteredTree.isFiltered();
		boolean addEnabled = true;
		boolean upEnabled = false;
		boolean downEnabled = false;
		if (filtered) {
			// Fix for bug 194529 and bug 194828
			addEnabled = false;
			upEnabled = false;
			downEnabled = false;
		}
		else {
			if (item instanceof IPluginElement) {
				IPluginElement element = (IPluginElement) item;
				IPluginParent parent = (IPluginParent) element.getParent();
				// check up
				int index = parent.getIndexOf(element);
				if (index > 0)
					upEnabled = true;
				if (index < parent.getChildCount() - 1)
					downEnabled = true;
			} else if (item instanceof IPluginExtension) {
				IPluginExtension extension = (IPluginExtension) item;
				IExtensions extensions = (IExtensions) extension.getParent();
				int index = extensions.getIndexOf(extension);
				int size = extensions.getExtensions().length;
				if (index > 0)
					upEnabled = true;
				if (index < size - 1)
					downEnabled = true;
			}
		}
		getTreePart().setButtonEnabled(0, addEnabled);
		getTreePart().setButtonEnabled(2, upEnabled);
		getTreePart().setButtonEnabled(3, downEnabled);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		fFilteredTree = new FormFilteredTree(parent, style, new PatternFilter());
		parent.setData("filtered", Boolean.TRUE); //$NON-NLS-1$
		return fFilteredTree.getViewer();
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			StructuredViewer viewer = getStructuredViewerPart().getViewer();
			IStructuredSelection ssel = (IStructuredSelection)viewer.getSelection();
			updateButtons(ssel.size() != 1 ? null : ssel.getFirstElement());
		}
	}

	protected void selectExtensionElement(ISelection selection) {
		fExtensionTree.setSelection(selection, true);
	}
}
