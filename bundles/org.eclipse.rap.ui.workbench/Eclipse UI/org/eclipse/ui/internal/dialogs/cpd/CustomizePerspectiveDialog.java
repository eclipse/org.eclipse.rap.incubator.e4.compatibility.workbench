/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Hochstein (Freescale) - Bug 407522 - Perspective reset not working correctly
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 422040, 431992, 472654
 *     Andrey Loskutov <loskutov@gmx.de> - Bug 456729, 404348, 421178, 420956, 424638, 460503
 *******************************************************************************/
package org.eclipse.ui.internal.dialogs.cpd;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.bindings.EBindingService;
import org.eclipse.e4.ui.internal.workbench.OpaqueElementUtil;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarSeparator;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.IResourceUtilities;
import org.eclipse.e4.ui.workbench.renderers.swt.MenuManagerRenderer;
import org.eclipse.e4.ui.workbench.renderers.swt.ToolBarManagerRenderer;
import org.eclipse.e4.ui.workbench.swt.util.ISWTResourceUtilities;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.internal.provisional.action.ToolBarContributionItem2;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.OpenPerspectiveAction;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.internal.ActionSetActionBars;
import org.eclipse.ui.internal.ActionSetContributionItem;
import org.eclipse.ui.internal.ActionSetMenuManager;
import org.eclipse.ui.internal.CoolBarToTrimManager;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.internal.Perspective;
import org.eclipse.ui.internal.PluginActionCoolBarContributionItem;
import org.eclipse.ui.internal.PluginActionSet;
import org.eclipse.ui.internal.PluginActionSetBuilder;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.actions.NewWizardShortcutAction;
import org.eclipse.ui.internal.dialogs.DialogUtil;
import org.eclipse.ui.internal.dialogs.WorkbenchWizardElement;
import org.eclipse.ui.internal.dialogs.cpd.TreeManager.TreeItem;
import org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout;
import org.eclipse.ui.internal.intro.IIntroConstants;
import org.eclipse.ui.internal.registry.ActionSetDescriptor;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.model.WorkbenchViewerComparator;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.IViewCategory;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.ui.wizards.IWizardCategory;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * Dialog to allow users the ability to customize the perspective. This includes
 * customizing menus and toolbars by adding, removing, or re-arranging commands
 * or groups of commands.
 *
 */
public class CustomizePerspectiveDialog extends TrayDialog {

	private static final String TOOLBAR_ICON = "$nl$/icons/full/obj16/toolbar.gif"; //$NON-NLS-1$
	private static final String SUBMENU_ICON = "$nl$/icons/full/obj16/submenu.gif"; //$NON-NLS-1$
	private static final String MENU_ICON = "$nl$/icons/full/obj16/menu.gif"; //$NON-NLS-1$
	private static final String WARNING_ICON = "$nl$/icons/full/obj16/warn_tsk.gif"; //$NON-NLS-1$

	private static final String SHORTCUT_CONTRIBUTION_ITEM_ID_OPEN_PERSPECTIVE = "openPerspective"; //$NON-NLS-1$
	private static final String SHORTCUT_CONTRIBUTION_ITEM_ID_SHOW_VIEW = "showView"; //$NON-NLS-1$

	static final String KEYS_PREFERENCE_PAGE_ID = "org.eclipse.ui.preferencePages.Keys"; //$NON-NLS-1$

	static final String NEW_LINE = System.getProperty("line.separator"); //$NON-NLS-1$

	static final int MIN_TOOLTIP_WIDTH = 160;

	WorkbenchWindow window;

	private WorkbenchPage windowPage;

	private Perspective perspective;

	private TabFolder tabFolder;

	private static final int TAB_WIDTH_IN_DLUS = 490;

	private static final int TAB_HEIGHT_IN_DLUS = 230;

	private final String shortcutMenuColumnHeaders[] = {
			WorkbenchMessages.get().ActionSetSelection_menuColumnHeader,
			WorkbenchMessages.get().ActionSetSelection_descriptionColumnHeader };

	private int[] shortcutMenuColumnWidths = { 125, 300 };

	ImageDescriptor menuImageDescriptor;

	ImageDescriptor submenuImageDescriptor;

	ImageDescriptor toolbarImageDescriptor;

	ImageDescriptor warningImageDescriptor;

	private TreeManager treeManager;

	private DisplayItem menuItems;

	private DisplayItem toolBarItems;

	private Category shortcuts;

	private DisplayItem wizards;

	private DisplayItem perspectives;

	private DisplayItem views;

	Map<String, ActionSet> idToActionSet = new HashMap<>();

	private final List<ActionSet> actionSets = new ArrayList<>();

	private IWorkbenchWindowConfigurer configurer;

	private TabItem actionSetTab;

	private CheckboxTableViewer actionSetAvailabilityTable;

	private TreeViewer actionSetMenuViewer;

	private TreeViewer actionSetToolbarViewer;

	private CheckboxTreeViewer menuStructureViewer1;

	private CheckboxTreeViewer menuStructureViewer2;

	private CheckboxTreeViewer toolbarStructureViewer1;

	private CheckboxTreeViewer toolbarStructureViewer2;

	private CustomizeActionBars customizeActionBars;

	private MenuManagerRenderer menuMngrRenderer;
	private ToolBarManagerRenderer toolbarMngrRenderer;

	private ISWTResourceUtilities resUtils;
	private IEclipseContext context;

	/**
	 * Represents a menu item or a tool bar item.
	 *
	 * @since 3.5
	 */
	class DisplayItem extends TreeItem {
		/** The logic item represented */
		private IContributionItem item;

		/** The action set this item belongs to (optional) */
		ActionSet actionSet;

		public DisplayItem(String label, IContributionItem item) {
			treeManager.super(label == null ? null : DialogUtil
					.removeAccel(removeShortcut(label)));
			this.item = item;
		}

		public void setActionSet(ActionSet actionSet) {
			this.actionSet = actionSet;
			if (actionSet != null) {
				actionSet.addItem(this);
			}
		}

		public ActionSet getActionSet() {
			return actionSet;
		}

		public IContributionItem getIContributionItem() {
			return item;
		}

		@Override
		public String toString() {
			return super.toString() + (item == null ? "" : (" [" + item.getId() + "]")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * Represents a menu item whose content is dynamic. Contains a list of the
	 * current items being displayed.
	 *
	 * @since 3.5
	 */
	class DynamicContributionItem extends DisplayItem {
		private List<MenuItem> preview;

		public DynamicContributionItem(IContributionItem item) {
			super(WorkbenchMessages.get().HideItems_dynamicItemName, item);
			preview = new ArrayList<>();
		}

		public void addCurrentItem(MenuItem item) {
			preview.add(item);
		}

		public List<MenuItem> getCurrentItems() {
			return preview;
		}
	}

	/**
	 * @param descriptor
	 * @param window
	 * @return the appropriate {@link IContributionItem} for the given wizard
	 */
	private static ActionContributionItem getIContributionItem(
			IWizardDescriptor descriptor, IWorkbenchWindow window) {
		IAction action = new NewWizardShortcutAction(window, descriptor);
		return new ActionContributionItem(action);
	}

	/**
	 * @param descriptor
	 * @param window
	 * @return the appropriate {@link IContributionItem} for the given
	 *         perspective
	 */
	private static ActionContributionItem getIContributionItem(
			IPerspectiveDescriptor descriptor, IWorkbenchWindow window) {
		IAction action = new OpenPerspectiveAction(window, descriptor, null);
		return new ActionContributionItem(action);
	}

	/**
	 * @param window
	 * @return the appropriate {@link IContributionItem} for showing views
	 */
	private static ActionContributionItem getIContributionItem(
			IWorkbenchWindow window) {
		IAction action = ActionFactory.SHOW_VIEW_MENU.create(window);
		return new ActionContributionItem(action);
	}

	/**
	 * Represents a menu item which needs to be shown in the Shortcuts tab.
	 *
	 * @since 3.5
	 */
	class ShortcutItem extends DisplayItem {
		/** The description to show in the table */
		private String description;

		/** The category this shortcut is in (should be set) */
		private Category category;

		private Object descriptor;

		public ShortcutItem(String label, IWizardDescriptor descriptor) {
			super(label, CustomizePerspectiveDialog.getIContributionItem(
					descriptor, window));
			this.descriptor = descriptor;
		}

		public ShortcutItem(String label, IPerspectiveDescriptor descriptor) {
			super(label, CustomizePerspectiveDialog.getIContributionItem(
					descriptor, window));
			this.descriptor = descriptor;
		}

		public ShortcutItem(String label, IViewDescriptor descriptor) {
			super(label, CustomizePerspectiveDialog
					.getIContributionItem(window));
			this.descriptor = descriptor;
		}

		public Object getDescriptor() {
			return descriptor;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setCategory(Category category) {
			this.category = category;
		}

		public Category getCategory() {
			return category;
		}
	}

	/**
	 * Represents a category in the shortcuts menu. Since categories can have a
	 * tree-structure, the functionality provided by the TreeManager and
	 * TreeItem classes is used, however the logic for visibility changes and
	 * gray states is more sophisticated.
	 *
	 * @since 3.5
	 */
	class Category extends TreeItem {

		/** ShortcutItems which are contributed in this Category */
		private List<ShortcutItem> contributionItems;

		public Category(String label) {
			treeManager.super(label == null ? null : DialogUtil
					.removeAccel(removeShortcut(label)));
			this.contributionItems = new ArrayList<>();
		}

		public List<ShortcutItem> getContributionItems() {
			return contributionItems;
		}

		/**
		 * Adds another ShortcutItem to this Category's list of ShortcutItems
		 * and creates a pseudo-child/parent relationship.
		 *
		 * @param item
		 *            the item to add
		 */
		public void addShortcutItem(ShortcutItem item) {
			contributionItems.add(item);
			item.setCategory(this);
		}

		/**
		 * While the child/parent state in the Category hierarchy is
		 * automatically maintained, the pseudo-child/parent relationship must
		 * be explicitly updated. This method will update Categories if their
		 * states need to change as a result of their ShortcutItems.
		 */
		public void update() {
			for (ShortcutItem shortcutItem : contributionItems) {
				DisplayItem item = shortcutItem;
				if (item.getState()) {
					this.setCheckState(true);
					return;
				}
			}

			this.setCheckState(false);
		}

		/**
		 * Changes the state of all pseudo-descendant ShortcutItems, causing the
		 * effective state of this Category and all its sub-Categories to match.
		 *
		 * @param state
		 *            The state to set this branch to.
		 */
		public void setItemsState(boolean state) {
			for (ShortcutItem shortcutItem : contributionItems) {
				shortcutItem.setCheckState(state);
			}

			for (Object o : getChildren()) {
				Category category = (Category) o;
				category.setItemsState(state);
			}
		}
	}

	/**
	 * Represents an action set, under which ContributionItems exist. There is
	 * no inherent hierarchy in action sets - they exist independent of one
	 * another, simply contribution menu items and tool bar items.
	 *
	 * @since 3.5
	 */
	class ActionSet {
		/** The descriptor which describes the action set represented */
		ActionSetDescriptor descriptor;

		/** ContributionItems contributed by this action set */
		private List<DisplayItem> contributionItems;

		private boolean active;

		private boolean wasChanged = false;

		public ActionSet(ActionSetDescriptor descriptor, boolean active) {
			this.descriptor = descriptor;
			this.active = active;
			this.contributionItems = new ArrayList<>();
		}

		public void addItem(DisplayItem item) {
			contributionItems.add(item);
		}

		@Override
		public String toString() {
			return descriptor.getLabel();
		}

		public boolean isActive() {
			return active;
		}

		public boolean wasChanged() {
			return wasChanged;
		}

		public void setActive(boolean active) {
			boolean wasActive = this.active;
			this.active = active;
			if (!active) {
				for (DisplayItem item : contributionItems) {
					item.setCheckState(false);
				}
			}
			if (wasActive != active) {
				actionSetAvailabilityChanged();
			}

			wasChanged = true;
		}
	}

	/**
	 * Create an instance of this Dialog.
	 *
	 * @param configurer
	 *            the configurer
	 * @param persp
	 *            the perspective
	 * @param context
	 *            The runtime context for this window
	 */
	public CustomizePerspectiveDialog(IWorkbenchWindowConfigurer configurer, Perspective persp,
			IEclipseContext context) {
		super(configurer.getWindow().getShell());
		this.treeManager = new TreeManager();
		this.configurer = configurer;
		this.context = context;
		perspective = persp;
		window = (WorkbenchWindow) configurer.getWindow();
		windowPage = (WorkbenchPage) window.getActivePage();
		menuMngrRenderer = context.get(MenuManagerRenderer.class);
		toolbarMngrRenderer = context.get(ToolBarManagerRenderer.class);
		resUtils = (ISWTResourceUtilities) context.get(IResourceUtilities.class);

		initializeIcons();

		initializeActionSetInput();
		loadMenuAndToolbarStructure();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		String title = perspective.getDesc().getLabel();

		title = NLS.bind(WorkbenchMessages.get().ActionSetSelection_customize, title);
		shell.setText(title);
		window.getWorkbench().getHelpSystem().setHelp(shell,
				IWorkbenchHelpContextIds.ACTION_SET_SELECTION_DIALOG);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {

		Button okButton = createButton(parent, IDialogConstants.OK_ID,
				WorkbenchMessages.get().CustomizePerspectiveDialog_okButtonLabel, true);
		okButton.setFocus();

		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.get().CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		// tab folder
		tabFolder = new TabFolder(composite, SWT.NONE);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = convertHorizontalDLUsToPixels(TAB_WIDTH_IN_DLUS);
		gd.heightHint = convertVerticalDLUsToPixels(TAB_HEIGHT_IN_DLUS);
		tabFolder.setLayoutData(gd);

		// Tool Bar Item Hiding Page
		TabItem tab = new TabItem(tabFolder, SWT.NONE);
		tab.setText(WorkbenchMessages.get().HideToolBarItems_toolBarItemsTab);
		tab.setControl(createToolBarVisibilityPage(tabFolder));

		// Menu Item Hiding Page
		tab = new TabItem(tabFolder, SWT.NONE);
		tab.setControl(createMenuVisibilityPage(tabFolder));
		tab.setText(WorkbenchMessages.get().HideMenuItems_menuItemsTab);

		// Action Set Availability Page
		actionSetTab = new TabItem(tabFolder, SWT.NONE);
		actionSetTab
				.setText(WorkbenchMessages.get().ActionSetSelection_actionSetsTab);
		actionSetTab.setControl(createActionSetAvailabilityPage(tabFolder));

		// Shortcuts Page
		if (showShortcutTab()) {
			TabItem item1 = new TabItem(tabFolder, SWT.NONE);
			item1.setText(WorkbenchMessages.get().Shortcuts_shortcutTab);
			item1.setControl(createShortCutsPage(tabFolder));
		}

		applyDialogFont(tabFolder);

		return composite;
	}

	private Composite createShortCutsPage(Composite parent) {
		GridData data;

		Composite menusComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		menusComposite.setLayout(layout);

		// Select... label
		Label label = new Label(menusComposite, SWT.WRAP);
		label.setText(NLS.bind(
				WorkbenchMessages.get().Shortcuts_selectShortcutsLabel, perspective
						.getDesc().getLabel()));
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(data);

		Label sep = new Label(menusComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		SashForm sashComposite = new SashForm(menusComposite, SWT.HORIZONTAL);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashComposite.setLayoutData(data);

		// Menus List
		Composite menusGroup = new Composite(sashComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		menusGroup.setLayout(layout);
		menusGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		label = new Label(menusGroup, SWT.WRAP);
		label.setText(WorkbenchMessages.get().Shortcuts_availableMenus);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Combo menusCombo = new Combo(menusGroup, SWT.READ_ONLY);
		menusCombo
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ComboViewer menusViewer = new ComboViewer(menusCombo);
		menusViewer.setContentProvider(TreeManager.getTreeContentProvider());
		menusViewer.setLabelProvider(TreeManager.getLabelProvider());

		// Categories Tree
		label = new Label(menusGroup, SWT.WRAP);
		label.setText(WorkbenchMessages.get().Shortcuts_availableCategories);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final CheckboxTreeViewer menuCategoriesViewer = new CheckboxTreeViewer(
				menusGroup);
		menuCategoriesViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		menuCategoriesViewer.setLabelProvider(TreeManager.getLabelProvider());
		menuCategoriesViewer.setContentProvider(TreeManager
				.getTreeContentProvider());
		menuCategoriesViewer.setComparator(new WorkbenchViewerComparator());
		menuCategoriesViewer.setCheckStateProvider(new CategoryCheckProvider());
		menuCategoriesViewer.addCheckStateListener(event -> {
			Category category = (Category) event.getElement();
			category.setItemsState(event.getChecked());
			updateCategoryAndParents(menuCategoriesViewer, category);
		});

		treeManager.addListener(changedItem -> {
			if (changedItem instanceof Category) {
				menuCategoriesViewer.update(changedItem, null);
			} else if (changedItem instanceof ShortcutItem) {
				ShortcutItem item = (ShortcutItem) changedItem;
				if (item.getCategory() != null) {
					item.getCategory().update();
					updateCategoryAndParents(menuCategoriesViewer, item
							.getCategory());
				}
			}
		});

		// Menu items list
		Composite menuItemsGroup = new Composite(sashComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		menuItemsGroup.setLayout(layout);
		menuItemsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));

		label = new Label(menuItemsGroup, SWT.WRAP);
		label.setText(WorkbenchMessages.get().Shortcuts_allShortcuts);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final CheckboxTableViewer menuItemsViewer = CheckboxTableViewer
				.newCheckList(menuItemsGroup, SWT.BORDER | SWT.H_SCROLL
						| SWT.V_SCROLL);
		Table menuTable = menuItemsViewer.getTable();
		menuTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		menuItemsViewer.setLabelProvider(new ShortcutLabelProvider());
		menuItemsViewer.setCheckStateProvider(TreeManager
				.getCheckStateProvider());
		menuItemsViewer.addCheckStateListener(treeManager
				.getViewerCheckStateListener());
		treeManager.getCheckListener(menuItemsViewer);

		menuItemsViewer
				.setContentProvider(new TreeManager.TreeItemContentProvider() {
					@Override
					public Object[] getChildren(Object parentElement) {
						if (parentElement instanceof Category) {
							return ((Category) parentElement)
									.getContributionItems().toArray();
						}
						return super.getChildren(parentElement);
					}
				});
		menuItemsViewer.setComparator(new WorkbenchViewerComparator());

		// update menuCategoriesViewer, and menuItemsViewer on a change to
		// menusViewer
		menusViewer
				.addSelectionChangedListener(event -> {
					Category category = (Category) event.getStructuredSelection().getFirstElement();
					menuCategoriesViewer.setInput(category);
					menuItemsViewer.setInput(category);
					if (category.getChildrenCount() != 0) {
						setSelectionOn(menuCategoriesViewer, category
								.getChildren().get(0));
					}
				});

		// update menuItemsViewer on a change to menuCategoriesViewer
		menuCategoriesViewer
				.addSelectionChangedListener(event -> {
					Category category = (Category) event.getStructuredSelection().getFirstElement();
					menuItemsViewer.setInput(category);
				});

		menuTable.setHeaderVisible(true);
		int[] columnWidths = new int[shortcutMenuColumnWidths.length];
		for (int i = 0; i < shortcutMenuColumnWidths.length; i++) {
			columnWidths[i] = convertHorizontalDLUsToPixels(shortcutMenuColumnWidths[i]);
		}
		for (int i = 0; i < shortcutMenuColumnHeaders.length; i++) {
			TableColumn tc = new TableColumn(menuTable, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(shortcutMenuColumnHeaders[i]);
			tc.setWidth(columnWidths[i]);
		}
		sashComposite.setWeights(new int[] { 30, 70 });

		menusViewer.setInput(shortcuts);

		if (shortcuts.getChildrenCount() > 0) {
			setSelectionOn(menusViewer, shortcuts.getChildren().get(0));
		}

		return menusComposite;
	}

	private Composite createActionSetAvailabilityPage(Composite parent) {
		GridData data;

		Composite actionSetsComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		actionSetsComposite.setLayout(layout);

		// Select... label
		Label label = new Label(actionSetsComposite, SWT.WRAP);
		label.setText(NLS.bind(
				WorkbenchMessages.get().ActionSetSelection_selectActionSetsLabel,
				perspective.getDesc().getLabel()));
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(data);

		Label sep = new Label(actionSetsComposite, SWT.HORIZONTAL
				| SWT.SEPARATOR);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		SashForm sashComposite = new SashForm(actionSetsComposite,
				SWT.HORIZONTAL);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashComposite.setLayoutData(data);

		// Action Set List Composite
		Composite actionSetGroup = new Composite(sashComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		actionSetGroup.setLayout(layout);
		actionSetGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));

		label = new Label(actionSetGroup, SWT.WRAP);
		label.setText(WorkbenchMessages.get().ActionSetSelection_availableActionSets);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final CheckboxTableViewer actionSetsViewer = CheckboxTableViewer
				.newCheckList(actionSetGroup, SWT.BORDER | SWT.H_SCROLL
						| SWT.V_SCROLL);
		actionSetAvailabilityTable = actionSetsViewer;
		actionSetsViewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		actionSetsViewer.setContentProvider(new ArrayContentProvider());
		actionSetsViewer.setComparator(new WorkbenchViewerComparator());
		actionSetsViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isChecked(Object element) {
				return ((ActionSet) element).isActive();
			}

			@Override
			public boolean isGrayed(Object element) {
				return false;
			}
		});
		actionSetsViewer.setInput(actionSets.toArray());

		Table table = actionSetsViewer.getTable();
		// RAP [bm] ToolTip
//		new TableToolTip(table);

		final ActionSet[] selectedActionSet = { null };

		// Filter to show only branches necessary for the selected action set.
		final ViewerFilter setFilter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (selectedActionSet[0] == null) {
					return false;
				}
				return includeInSetStructure((DisplayItem) element,
						selectedActionSet[0]);
			}
		};

		// Updates the check state of action sets
		actionSetsViewer.addCheckStateListener(event -> {
			final ActionSet actionSet = (ActionSet) event.getElement();
			if (event.getChecked()) {
				actionSet.setActive(true);
				for (DisplayItem item : actionSet.contributionItems) {
					item.setCheckState(true);
				}
			} else {
				actionSet.setActive(false);
			}
		});

		// Menu and toolbar composite
		Composite actionGroup = new Composite(sashComposite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		actionGroup.setLayout(layout);
		actionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite menubarGroup = new Composite(actionGroup, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		menubarGroup.setLayout(layout);
		menubarGroup
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		label = new Label(menubarGroup, SWT.WRAP);
		label.setText(WorkbenchMessages.get().ActionSetSelection_menubarActions);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		actionSetMenuViewer = new TreeViewer(menubarGroup);
		actionSetMenuViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		actionSetMenuViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		actionSetMenuViewer.setUseHashlookup(true);
		actionSetMenuViewer.setContentProvider(TreeManager
				.getTreeContentProvider());
		actionSetMenuViewer.setLabelProvider(new GrayOutUnavailableLabelProvider(null));
		actionSetMenuViewer.addFilter(setFilter);
		actionSetMenuViewer.setInput(menuItems);

		Tree tree = actionSetMenuViewer.getTree();
		// RAP [bm] ToolTip
//		new ItemDetailToolTip(this, actionSetMenuViewer, tree, false, true, setFilter);

		Composite toolbarGroup = new Composite(actionGroup, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		toolbarGroup.setLayout(layout);
		toolbarGroup
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		label = new Label(toolbarGroup, SWT.WRAP);
		label.setText(WorkbenchMessages.get().ActionSetSelection_toolbarActions);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		actionSetToolbarViewer = new TreeViewer(toolbarGroup);
		actionSetToolbarViewer
				.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		actionSetToolbarViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		actionSetToolbarViewer.setContentProvider(TreeManager
				.getTreeContentProvider());
		actionSetToolbarViewer.setLabelProvider(new GrayOutUnavailableLabelProvider(null));
		actionSetToolbarViewer.addFilter(setFilter);
		actionSetToolbarViewer.setInput(toolBarItems);

		tree = actionSetToolbarViewer.getTree();
		// RAP [bm] ToolTip
//		new ItemDetailToolTip(this, actionSetToolbarViewer, tree, false, true, setFilter);

		// Updates the menu item and toolbar items tree viewers when the
		// selection changes
		actionSetsViewer
				.addSelectionChangedListener(event -> {
					selectedActionSet[0] = (ActionSet) event.getStructuredSelection().getFirstElement();
					actionSetMenuViewer.setInput(menuItems);
					actionSetToolbarViewer.setInput(toolBarItems);
				});

		sashComposite.setWeights(new int[] { 30, 70 });

		return actionSetsComposite;
	}

	/**
	 * Creates the page used to allow users to choose menu items to hide.
	 */
	private Composite createMenuVisibilityPage(Composite parent) {
		GridData data;

		Composite hideMenuItemsComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		hideMenuItemsComposite.setLayout(layout);

		// Label for entire tab
		Label label = new Label(hideMenuItemsComposite, SWT.WRAP);
		label.setText(WorkbenchMessages.get().HideMenuItems_chooseMenuItemsLabel);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(data);

		Label sep = new Label(hideMenuItemsComposite, SWT.HORIZONTAL
				| SWT.SEPARATOR);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Main contents of tab
		final PageBook book = new PageBook(hideMenuItemsComposite, SWT.NONE);
		data = new GridData(GridData.FILL_BOTH);
		book.setLayoutData(data);

		// Simple view: just the menu structure
		final Composite simpleComposite = createItemStructureGroup(book,
				WorkbenchMessages.get().HideMenuItems_menuStructure);
		menuStructureViewer1 = initStructureViewer(simpleComposite,
				new TreeManager.ViewerCheckStateListener(), null);

		// Update the viewer when the model changes
		treeManager.getCheckListener(menuStructureViewer1); // To update ctv on
															// model changes

		// Simply grab the checkstate out of the model
		menuStructureViewer1.setCheckStateProvider(TreeManager
				.getCheckStateProvider());

		// Init with input
		menuStructureViewer1.setInput(menuItems);

		// Advanced view: action set with filtered menu structure
		final SashForm advancedComposite = new SashForm(book, SWT.HORIZONTAL);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		advancedComposite.setLayoutData(data);

		// Action set list
		final TableViewer actionSetViewer = initActionSetViewer(createActionSetGroup(advancedComposite));

		// Filter to only show action sets which have useful menu items
		actionSetViewer.addFilter(new ShowUsedActionSetsFilter(menuItems));

		// Init with input
		actionSetViewer.setInput(actionSets.toArray());

		// Filter to only show items in the current action set
		final ActionSetFilter menuStructureFilterByActionSet = new ActionSetFilter();

		final Composite menuStructureComposite = createItemStructureGroup(
				advancedComposite,
				WorkbenchMessages.get().HideMenuItems_menuStructure);
		final ICheckStateListener menuStructureFilter = new FilteredViewerCheckListener(
				TreeManager.getTreeContentProvider(),
				menuStructureFilterByActionSet);
		menuStructureViewer2 = initStructureViewer(menuStructureComposite,
				menuStructureFilter, menuStructureFilterByActionSet);

		treeManager.addListener(new FilteredModelCheckListener(
				menuStructureFilterByActionSet, menuStructureViewer2));

		menuStructureViewer2.addFilter(menuStructureFilterByActionSet);

		// Update filter when a new action set is selected
		actionSetViewer
				.addSelectionChangedListener(new ActionSetSelectionChangedListener(
						menuStructureViewer2, menuStructureFilterByActionSet));

		// Check state provider to emulate standard SWT
		// behaviour on visual tree
		menuStructureViewer2
				.setCheckStateProvider(new FilteredTreeCheckProvider(
						TreeManager.getTreeContentProvider(),
						menuStructureFilterByActionSet));

		// Init input
		menuStructureViewer2.setInput(menuItems);

		// Override any attempts to set an item to visible
		// which exists in an unavailable action set
		treeManager.addListener(changedItem -> {
			if (!(changedItem instanceof DisplayItem)) {
				return;
			}
			if (!changedItem.getState()) {
				return;
			}
			if (isAvailable((DisplayItem) changedItem)) {
				return;
			}
			changedItem.setCheckState(false);
		});

		final Button showCommandGroupFilterButton = new Button(
				hideMenuItemsComposite, SWT.CHECK);
		showCommandGroupFilterButton
				.setText(WorkbenchMessages.get().HideItems_turnOnActionSets);
		showCommandGroupFilterButton
				.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						if (showCommandGroupFilterButton.getSelection()) {
							Object o = ((StructuredSelection) menuStructureViewer1
									.getSelection()).getFirstElement();
							ActionSet initSelectAS = null;
							DisplayItem initSelectCI = null;
							if (o instanceof DisplayItem) {
								initSelectCI = ((DisplayItem) o);
								initSelectAS = initSelectCI.getActionSet();
							}
							if (initSelectAS == null) {
								initSelectAS = (ActionSet) actionSetViewer
										.getElementAt(0);
							}
							if (initSelectAS != null) {
								setSelectionOn(actionSetViewer, initSelectAS);
								actionSetViewer.reveal(initSelectAS);
							}
							if (initSelectCI != null) {
								setSelectionOn(menuStructureViewer2,
										initSelectCI);
								menuStructureViewer2.reveal(initSelectCI);
							}
							book.showPage(advancedComposite);
						} else {
							book.showPage(simpleComposite);
						}
					}
				});

		book.showPage(simpleComposite);
		advancedComposite.setWeights(new int[] { 30, 70 });

		return hideMenuItemsComposite;
	}

	/**
	 * Creates the page used to allow users to choose menu items to hide.
	 */
	private Composite createToolBarVisibilityPage(Composite parent) {
		GridData data;

		Composite hideToolbarItemsComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		hideToolbarItemsComposite.setLayout(layout);

		// Label for entire tab
		Label label = new Label(hideToolbarItemsComposite, SWT.WRAP);
		label
				.setText(WorkbenchMessages.get().HideToolBarItems_chooseToolBarItemsLabel);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(data);

		Label sep = new Label(hideToolbarItemsComposite, SWT.HORIZONTAL
				| SWT.SEPARATOR);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Main contents of tab
		final PageBook book = new PageBook(hideToolbarItemsComposite, SWT.NONE);
		data = new GridData(GridData.FILL_BOTH);
		book.setLayoutData(data);

		// Simple view: just the toolbar structure
		final Composite simpleComposite = createItemStructureGroup(book,
				WorkbenchMessages.get().HideToolBarItems_toolBarStructure);
		toolbarStructureViewer1 = initStructureViewer(simpleComposite,
				new TreeManager.ViewerCheckStateListener(), null);

		// Update the viewer when the model changes
		treeManager.getCheckListener(toolbarStructureViewer1); // To update ctv
																// on model
																// changes

		// Simply grab the check state out of the model
		toolbarStructureViewer1.setCheckStateProvider(TreeManager
				.getCheckStateProvider());

		// Init with input
		toolbarStructureViewer1.setInput(toolBarItems);

		// Advanced view: action set with filtered toolbar structure
		final SashForm advancedComposite = new SashForm(book, SWT.HORIZONTAL);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		advancedComposite.setLayoutData(data);

		// Action set list
		final TableViewer actionSetViewer = initActionSetViewer(createActionSetGroup(advancedComposite));

		// Filter to only show action sets which have useful toolbar items
		actionSetViewer.addFilter(new ShowUsedActionSetsFilter(toolBarItems));

		// Init with input
		actionSetViewer.setInput(actionSets.toArray());

		// Filter to only show items in the current action set
		final ActionSetFilter toolbarStructureFilterByActionSet = new ActionSetFilter();

		final Composite toolbarStructureComposite = createItemStructureGroup(
				advancedComposite,
				WorkbenchMessages.get().HideToolBarItems_toolBarStructure);
		final ICheckStateListener toolbarStructureFilter = new FilteredViewerCheckListener(
				TreeManager.getTreeContentProvider(),
				toolbarStructureFilterByActionSet);
		toolbarStructureViewer2 = initStructureViewer(
				toolbarStructureComposite, toolbarStructureFilter,
				toolbarStructureFilterByActionSet);

		toolbarStructureViewer2.addFilter(toolbarStructureFilterByActionSet);

		treeManager.addListener(new FilteredModelCheckListener(
				toolbarStructureFilterByActionSet, toolbarStructureViewer2));

		// Update filter when a new action set is selected
		actionSetViewer
				.addSelectionChangedListener(new ActionSetSelectionChangedListener(
						toolbarStructureViewer2,
						toolbarStructureFilterByActionSet));

		// Check state provider to emulate standard SWT
		// behaviour on visual tree
		toolbarStructureViewer2
				.setCheckStateProvider(new FilteredTreeCheckProvider(
						TreeManager.getTreeContentProvider(),
						toolbarStructureFilterByActionSet));

		// Init input
		toolbarStructureViewer2.setInput(toolBarItems);

		// Override any attempts to set an item to visible
		// which exists in an unavailable action set
		treeManager.addListener(changedItem -> {
			if (!(changedItem instanceof DisplayItem)) {
				return;
			}
			if (!changedItem.getState()) {
				return;
			}
			if (isAvailable((DisplayItem) changedItem)) {
				return;
			}
			changedItem.setCheckState(false);
		});

		final Button showCommandGroupFilterButton = new Button(
				hideToolbarItemsComposite, SWT.CHECK);
		showCommandGroupFilterButton
				.setText(WorkbenchMessages.get().HideItems_turnOnActionSets);
		showCommandGroupFilterButton
				.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						if (showCommandGroupFilterButton.getSelection()) {
							Object o = ((StructuredSelection) toolbarStructureViewer1
									.getSelection()).getFirstElement();
							ActionSet initSelectAS = null;
							DisplayItem initSelectCI = null;
							if (o instanceof DisplayItem) {
								initSelectCI = ((DisplayItem) o);
								initSelectAS = initSelectCI.getActionSet();
							}
							if (initSelectAS == null) {
								initSelectAS = (ActionSet) actionSetViewer
										.getElementAt(0);
							}
							if (initSelectAS != null) {
								setSelectionOn(actionSetViewer, initSelectAS);
								actionSetViewer.reveal(initSelectAS);
							}
							if (initSelectCI != null) {
								setSelectionOn(toolbarStructureViewer2,
										initSelectCI);
								toolbarStructureViewer2.reveal(initSelectCI);
							}
							book.showPage(advancedComposite);
						} else {
							book.showPage(simpleComposite);
						}
					}
				});

		book.showPage(simpleComposite);
		advancedComposite.setWeights(new int[] { 30, 70 });

		return hideToolbarItemsComposite;
	}

	/**
	 * Creates a table to display action sets.
	 *
	 * @param parent
	 * @return a viewer to display action sets
	 */
	private static TableViewer initActionSetViewer(Composite parent) {
		// List of categories
		final TableViewer actionSetViewer = new TableViewer(parent, SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL);
		actionSetViewer.getTable().setLayoutData(
				new GridData(GridData.FILL_BOTH));
		actionSetViewer.setLabelProvider(new GrayOutUnavailableLabelProvider(null));
		actionSetViewer.setComparator(new WorkbenchViewerComparator());
		actionSetViewer.setContentProvider(new ArrayContentProvider());

		// Tooltip on tree items
		Table table = actionSetViewer.getTable();
		// RAP [bm] ToolTip
//		new TableToolTip(table);
		return actionSetViewer;
	}

	/**
	 * Creates a CheckboxTreeViewer to display menu or toolbar structure.
	 *
	 * @param parent
	 * @param checkStateListener
	 *            the listener which listens to the viewer for check changes
	 * @param filter
	 *            the filter used in the viewer (null for none)
	 * @return A viewer within <code>parent</code> which will show menu or
	 *         toolbar structure. It comes setup, only missing a
	 *         CheckStateProvider and its input.
	 */
	private CheckboxTreeViewer initStructureViewer(Composite parent,
			ICheckStateListener checkStateListener, ViewerFilter filter) {
		CheckboxTreeViewer ctv = new CheckboxTreeViewer(parent, SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL);
		ctv.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		ctv.setUseHashlookup(true);
		ctv.setContentProvider(TreeManager.getTreeContentProvider());
		// use an UnavailableContributionItemCheckListener to filter check
		// events: if it is legal, forward it to the actual checkStateListener,
		// if not, inform the user
		ctv.addCheckStateListener(new UnavailableContributionItemCheckListener(
				this, ctv, checkStateListener));
		ctv.setLabelProvider(new GrayOutUnavailableLabelProvider(filter));
		// RAP [bm] ToolTip
//		new ItemDetailToolTip(this, ctv, ctv.getTree(), true, true, filter);
		return ctv;
	}

	/**
	 * Creates a composite to put a tree viewer in to display menu or toolbar
	 * items.
	 */
	private static Composite createItemStructureGroup(
			final Composite composite, String labelText) {
		GridLayout layout;
		Label label;
		layout = new GridLayout();
		Composite menubarGroup = new Composite(composite, SWT.NONE);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		menubarGroup.setLayout(layout);
		menubarGroup
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		label = new Label(menubarGroup, SWT.WRAP);
		label.setText(labelText);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		return menubarGroup;
	}

	/**
	 * Creates a composite to put a viewer in to display action sets.
	 */
	private static Composite createActionSetGroup(final Composite composite) {
		GridLayout layout;
		Label label;
		Composite actionSetGroup = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		actionSetGroup.setLayout(layout);
		actionSetGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));

		label = new Label(actionSetGroup, SWT.WRAP);
		label.setText(WorkbenchMessages.get().HideItems_commandGroupTitle);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		return actionSetGroup;
	}

	/**
	 * Set the selection on a structured viewer.
	 *
	 * @param viewer
	 * @param selected
	 */
	private static void setSelectionOn(Viewer viewer, final Object selected) {
		ISelection selection;
		if (selected == null) {
			selection = StructuredSelection.EMPTY;
		} else {
			selection = new StructuredSelection(selected);
		}
		boolean reveal = selection != StructuredSelection.EMPTY;
		viewer.setSelection(selection, reveal);
	}

	/**
	 * Searches deeply to see if <code>item</code> is a node in a branch
	 * containing a ContributionItem contributed by <code>set</code>.
	 *
	 * @param item
	 *            the item in question
	 * @param set
	 *            the action set to look for
	 * @return true iff <code>item</code> is required in build a tree including
	 *         elements in <code>set</code>
	 */
	static boolean includeInSetStructure(DisplayItem item, ActionSet set) {
		if (item.actionSet != null && item.actionSet.equals(set)) {
			return true;
		}
		for (TreeItem treeItem : item.getChildren()) {
			DisplayItem child = (DisplayItem) treeItem;
			if (includeInSetStructure(child, set)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param item
	 * @return true iff the item is available - i.e. if it belongs to an action
	 *         set, that that action set is available, or has a child which is
	 *         available thus must be displayed in order to display the child
	 */
	static boolean isAvailable(DisplayItem item) {
		if (item.getActionSet() != null && item.getChildren().isEmpty()) {
			return item.getActionSet().isActive();
		}
		for (TreeItem treeItem : item.getChildren()) {
			DisplayItem child = (DisplayItem) treeItem;
			if (isAvailable(child)) {
				return true;
			}
		}
		return item.getIContributionItem() != null && item.getIContributionItem().isVisible();
	}

	/**
	 * @param item
	 * @return true iff the item will show up in a menu or tool bar structure -
	 *         i.e. it is available, or has a child which is available thus must
	 *         be displayed in order to display the child
	 */
	static boolean isEffectivelyAvailable(DisplayItem item, ViewerFilter filter) {
		if (!isAvailable(item)) {
			return false;
		}
		final List<TreeItem> children = item.getChildren();
		if (children.isEmpty()) {
			return true;
		}
		for (TreeItem treeItem : children) {
			DisplayItem child = (DisplayItem) treeItem;
			if(filter != null && !filter.select(null, null, child)) {
				continue;
			}
			if (isAvailable(child)) {
				return true;
			}
		}
		for (TreeItem treeItem : children) {
			DisplayItem child = (DisplayItem) treeItem;
			if(filter != null && !filter.select(null, null, child)) {
				continue;
			}
			if (isEffectivelyAvailable(child, filter)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * On a change to availability, updates the appropriate widgets.
	 */
	private void actionSetAvailabilityChanged() {
		actionSetAvailabilityTable.refresh();
		actionSetMenuViewer.refresh();
		actionSetToolbarViewer.refresh();

		menuStructureViewer1.refresh();
		menuStructureViewer2.refresh();
		toolbarStructureViewer1.refresh();
		toolbarStructureViewer2.refresh();
	}

	private void initializeActionSetInput() {
		// Just get the action sets at this point. Do not load the action set
		// until it is actually selected in the dialog.
		ActionSetRegistry reg = WorkbenchPlugin.getDefault()
				.getActionSetRegistry();
		IActionSetDescriptor[] sets = reg.getActionSets();
		IActionSetDescriptor[] actionSetDescriptors = ((WorkbenchPage) window
				.getActivePage()).getActionSets();
		List<IActionSetDescriptor> initiallyAvailableActionSets = Arrays.asList(actionSetDescriptors);

		for (IActionSetDescriptor set : sets) {
			ActionSetDescriptor actionSetDesc = (ActionSetDescriptor) set;
			if (WorkbenchActivityHelper.filterItem(actionSetDesc)) {
				continue;
			}
			ActionSet actionSet = new ActionSet(actionSetDesc,
					initiallyAvailableActionSets.contains(actionSetDesc));
			idToActionSet.put(actionSetDesc.getId(), actionSet);
			actionSets.add(actionSet);
		}
	}

	private String getToolbarLabel(MUIElement elt) {
		MApplication app = context.get(MApplication.class);
		String toolbarLabel = CoolBarToTrimManager.getToolbarLabel(app, elt);
		if (toolbarLabel != null) {
			return toolbarLabel;
		}
		String elementId = elt.getElementId();
		ActionSetRegistry registry = WorkbenchPlugin.getDefault().getActionSetRegistry();
		IActionSetDescriptor findActionSet = registry.findActionSet(elementId);
		if (findActionSet != null) {
			return findActionSet.getLabel();
		}
		// Nothing is available. Let's smartly guess the name then.
		String[] nameParts = elementId.split("\\."); //$NON-NLS-1$
		return nameParts[nameParts.length - 1];
	}

	private void initializeIcons() {
		String iconPath = MENU_ICON;
		URL url = BundleUtility.find(PlatformUI.PLUGIN_ID, iconPath);
		menuImageDescriptor = ImageDescriptor.createFromURL(url);

		iconPath = SUBMENU_ICON;
		url = BundleUtility.find(PlatformUI.PLUGIN_ID, iconPath);
		submenuImageDescriptor = ImageDescriptor.createFromURL(url);

		iconPath = TOOLBAR_ICON;
		url = BundleUtility.find(PlatformUI.PLUGIN_ID, iconPath);
		toolbarImageDescriptor = ImageDescriptor.createFromURL(url);

		iconPath = WARNING_ICON;
		url = BundleUtility.find(PlatformUI.PLUGIN_ID, iconPath);
		warningImageDescriptor = ImageDescriptor.createFromURL(url);
	}

	private void initializeNewWizardsMenu(DisplayItem menu,
			Category parentCategory, IWizardCategory element, List<String> activeIds) {
		Category category = new Category(element.getLabel());
		parentCategory.addChild(category);

		Object[] wizards = element.getWizards();
		for (Object wizard2 : wizards) {
			WorkbenchWizardElement wizard = (WorkbenchWizardElement) wizard2;

			ShortcutItem item = new ShortcutItem(wizard.getLabel(), wizard);
			item.setLabel(wizard.getLabel());
			item.setDescription(wizard.getDescription());
			if (wizard.getImageDescriptor() != null) {
				item.setImageDescriptor(wizard.getImageDescriptor());
			}
			item.setCheckState(activeIds.contains(wizard.getId()));
			menu.addChild(item);
			category.addShortcutItem(item);
		}
		// @issue should not pass in null
		for (IWizardCategory child : element.getCategories()) {
			initializeNewWizardsMenu(menu, category, child, activeIds);
		}
	}

	private void initializeNewWizardsMenu(DisplayItem menu) {
		Category rootForNewWizards = new Category(
				WorkbenchMessages.get().ActionSetDialogInput_wizardCategory);
		shortcuts.addChild(rootForNewWizards);

		IWizardCategory wizardCollection = WorkbenchPlugin.getDefault()
				.getNewWizardRegistry().getRootCategory();
		IWizardCategory[] wizardCategories = wizardCollection.getCategories();
		List<String> activeIDs = Arrays.asList(perspective.getNewWizardShortcuts());

		for (IWizardCategory element : wizardCategories) {
			if (WorkbenchActivityHelper.filterItem(element)) {
				continue;
			}
			initializeNewWizardsMenu(menu, rootForNewWizards, element,
					activeIDs);
		}
	}

	private void initializePerspectivesMenu(DisplayItem menu) {
		Category rootForPerspectives = new Category(
				WorkbenchMessages.get().ActionSetDialogInput_perspectiveCategory);
		shortcuts.addChild(rootForPerspectives);

		IPerspectiveRegistry perspReg = WorkbenchPlugin.getDefault()
				.getPerspectiveRegistry();
		IPerspectiveDescriptor[] persps = perspReg.getPerspectives();

		List<String> activeIds = Arrays.asList(perspective.getPerspectiveShortcuts());

		for (IPerspectiveDescriptor perspective : persps) {
			if (WorkbenchActivityHelper.filterItem(perspective)) {
				continue;
			}

			ShortcutItem child = new ShortcutItem(perspective.getLabel(),
					perspective);
			child.setImageDescriptor(perspective.getImageDescriptor());
			child.setDescription(perspective.getDescription());
			child.setCheckState(activeIds.contains(perspective.getId()));
			menu.addChild(child);

			rootForPerspectives.addShortcutItem(child);
		}
	}

	private void initializeViewsMenu(DisplayItem menu) {
		Category rootForViews = new Category(
				WorkbenchMessages.get().ActionSetDialogInput_viewCategory);

		shortcuts.addChild(rootForViews);

		IViewRegistry viewReg = WorkbenchPlugin.getDefault().getViewRegistry();
		IViewCategory[] categories = viewReg.getCategories();

		List<String> activeIds = Arrays.asList(perspective.getShowViewShortcuts());

		for (IViewCategory category : categories) {
			if (WorkbenchActivityHelper.filterItem(category)) {
				continue;
			}

			Category viewCategory = new Category(category.getLabel());
			rootForViews.addChild(viewCategory);

			IViewDescriptor[] views = category.getViews();

			if (views != null) {
				for (IViewDescriptor view : views) {
					if (view.getId().equals(IIntroConstants.INTRO_VIEW_ID)) {
						continue;
					}
					if (WorkbenchActivityHelper.filterItem(view)) {
						continue;
					}

					ShortcutItem child = new ShortcutItem(view.getLabel(), view);
					child.setImageDescriptor(view.getImageDescriptor());
					child.setDescription(view.getDescription());
					child.setCheckState(activeIds.contains(view.getId()));
					menu.addChild(child);
					viewCategory.addShortcutItem(child);
				}
			}
		}
	}

	/**
	 * Loads the current perspective's menu structure and also loads which menu
	 * items are visible and not.
	 */
	private void loadMenuAndToolbarStructure() {
		customizeActionBars = new CustomizeActionBars(configurer, context);

		// Fill fake action bars with static menu information.
		window.fillActionBars(customizeActionBars, ActionBarAdvisor.FILL_PROXY
				| ActionBarAdvisor.FILL_MENU_BAR
				| ActionBarAdvisor.FILL_COOL_BAR);

		window.fill(customizeActionBars.menuRenderer,
				 customizeActionBars.mainMenu, customizeActionBars.menuManager);

		// Populate the action bars with the action sets' data
		for (ActionSet actionSet : actionSets) {
			ActionSetDescriptor descriptor = actionSet.descriptor;
			PluginActionSet pluginActionSet = buildMenusAndToolbarsFor(
					customizeActionBars, descriptor);

			if (pluginActionSet != null) {
				pluginActionSet.dispose();
			}
		}

		// Add actionSet MenuManagers to menu
		MenuManager menuManager = customizeActionBars.menuManager;
		IContributionItem[] items = menuManager.getItems();
		for (IContributionItem item : items) {
			if (item instanceof ActionSetContributionItem) {
				ActionSetContributionItem asci = (ActionSetContributionItem) item;
				menuManager.add(asci.getInnerItem());
			}
		}

		// Make all menu items visible so they are included in the list.
		customizeActionBars.menuManager.setVisible(true);

		makeAllContributionsVisible(customizeActionBars.menuManager);

		customizeActionBars.menuRenderer.reconcileManagerToModel(customizeActionBars.menuManager,
				customizeActionBars.mainMenu);

		IPresentationEngine engine = context.get(IPresentationEngine.class);
		engine.createGui(customizeActionBars.mainMenu, customizeActionBars.windowModel.getWidget(),
				customizeActionBars.windowModel.getContext());

		MTrimBar topTrim = customizeActionBars.coolBarManager.getTopTrim();
		topTrim.setToBeRendered(true);

		// Get the menu from the action bars
		engine.createGui(topTrim, customizeActionBars.windowModel.getWidget(),
				customizeActionBars.windowModel.getContext());

		// Ensure the menu is completely built by updating the menu manager.
		// (This method call requires a menu already be created)
		customizeActionBars.menuManager.updateAll(true);
		customizeActionBars.coolBarManager.update(true);

		shortcuts = new Category(""); //$NON-NLS-1$
		toolBarItems = createTrimBarEntries(topTrim);
		menuItems = createMenuStructure(customizeActionBars.mainMenu);
	}

	private PluginActionSet buildMenusAndToolbarsFor(
			CustomizeActionBars customizeActionBars,
			ActionSetDescriptor actionSetDesc) {
		String id = actionSetDesc.getId();
		ActionSetActionBars bars = new ActionSetActionBars(customizeActionBars,
				window, customizeActionBars, id);
		bars.getMenuManager().setVisible(true);
		PluginActionSetBuilder builder = new PluginActionSetBuilder();
		PluginActionSet actionSet = null;
		try {
			actionSet = (PluginActionSet) actionSetDesc.createActionSet();
			actionSet.init(null, bars);
		} catch (CoreException ex) {
			WorkbenchPlugin.log(
					"Unable to create action set " + actionSetDesc.getId(), ex); //$NON-NLS-1$
			return null;
		}
		builder.buildMenuAndToolBarStructure(actionSet, window);
		return actionSet;
	}

	/**
	 * @return can return null
	 */
	static String getCommandID(DisplayItem item) {
		Object object = item.getIContributionItem();

		if (item instanceof ShortcutItem && isShowView(item)) {
			return IWorkbenchCommandConstants.VIEWS_SHOW_VIEW;
		}

		return getIDFromIContributionItem(object);
	}

	/**
	 * Given an object, tries to find an id which will uniquely identify it.
	 *
	 * @param object
	 *            an instance of {@link IContributionItem},
	 *            {@link IPerspectiveDescriptor}, {@link IViewDescriptor} or
	 *            {@link WorkbenchWizardElement}.
	 * @return an id, can return null
	 * @throws IllegalArgumentException
	 *             if object is not one of the listed types
	 */
	public static String getIDFromIContributionItem(Object object) {
		if (object instanceof ActionContributionItem) {
			ActionContributionItem item = (ActionContributionItem) object;
			IAction action = item.getAction();
			if (action == null) {
				return null;
			}
			if (action instanceof NewWizardShortcutAction) {
				return IWorkbenchCommandConstants.FILE_NEW;
			}
			if (action instanceof OpenPerspectiveAction) {
				return IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE;
			}
			String id = action.getActionDefinitionId();
			if (id != null) {
				return id;
			}
			return action.getId();
		}
		if (object instanceof ActionSetContributionItem) {
			ActionSetContributionItem item = (ActionSetContributionItem) object;
			IContributionItem subitem = item.getInnerItem();
			return getIDFromIContributionItem(subitem);
		}
		if (object instanceof CommandContributionItem) {
			CommandContributionItem item = (CommandContributionItem) object;
			ParameterizedCommand command = item.getCommand();
			if (command == null) {
				return null;
			}
			return command.getId();
		}
		if (object instanceof IPerspectiveDescriptor) {
			return ((IPerspectiveDescriptor) object).getId();
		}
		if (object instanceof IViewDescriptor) {
			return ((IViewDescriptor) object).getId();
		}
		if (object instanceof WorkbenchWizardElement) {
			return ((WorkbenchWizardElement) object).getLocalId();
		}
		if (object instanceof IContributionItem) {
			String id = ((IContributionItem) object).getId();
			if (id != null) {
				return id;
			}
			return object.getClass().getName();
		}
		return null;	//couldn't determine the id
	}

	static String getParamID(DisplayItem object) {
		if (object instanceof ShortcutItem) {
			ShortcutItem shortcutItem = (ShortcutItem) object;

			if (isNewWizard(shortcutItem)) {
				ActionContributionItem item = (ActionContributionItem) object
						.getIContributionItem();
				NewWizardShortcutAction nwsa = (NewWizardShortcutAction) item
						.getAction();
				return nwsa.getLocalId();
			}

			if (isShowPerspective(shortcutItem)) {
				ActionContributionItem item = (ActionContributionItem) object
						.getIContributionItem();
				OpenPerspectiveAction opa = (OpenPerspectiveAction) item
						.getAction();
				return opa.getLocalId();
			}

			if (isShowView(shortcutItem)) {
				IViewDescriptor descriptor = (IViewDescriptor) shortcutItem
						.getDescriptor();
				return descriptor.getId();
			}
		}

		return null;
	}

	static boolean isNewWizard(DisplayItem item) {
		if (!(item instanceof ShortcutItem)) {
			return false;
		}
		return ((ShortcutItem) item).getDescriptor() instanceof IWizardDescriptor;
	}

	static boolean isShowPerspective(DisplayItem item) {
		if (!(item instanceof ShortcutItem)) {
			return false;
		}
		return ((ShortcutItem) item).getDescriptor() instanceof IPerspectiveDescriptor;
	}

	static boolean isShowView(DisplayItem item) {
		if (!(item instanceof ShortcutItem)) {
			return false;
		}
		return ((ShortcutItem) item).getDescriptor() instanceof IViewDescriptor;
	}

	private static String getActionSetID(IContributionItem item) {
		if (item instanceof ActionSetContributionItem) {
			return ((ActionSetContributionItem) item).getActionSetId();
		}
		if (item instanceof PluginActionCoolBarContributionItem) {
			return ((PluginActionCoolBarContributionItem) item).getActionSetId();
		}
		if (item instanceof ContributionItem) {
			IContributionManager parent = ((ContributionItem) item).getParent();
			if (parent instanceof ActionSetMenuManager) {
				return ((ActionSetMenuManager) parent).getActionSetId();
			}
			if (item instanceof ToolBarContributionItem2) {
				return item.getId();
			}
		}
		return null;
	}

	private static String getActionSetID(MUIElement item) {
		String id = (String) item.getTransientData().get("ActionSet"); //$NON-NLS-1$
		if (id != null) {
			return id;
		}
		Object data = OpaqueElementUtil.getOpaqueItem(item);
		if (data == null) {
			data = item.getTransientData().get(CoolBarToTrimManager.OBJECT);
		}
		if (data instanceof IContributionItem) {
			return getActionSetID((IContributionItem) data);
		}
		return null;
	}

	/**
	 * Causes all items under the manager to be visible, so they can be read.
	 *
	 * @param manager
	 */
	private static void makeAllContributionsVisible(IContributionManager manager) {
		IContributionItem[] items = manager.getItems();

		for (IContributionItem item : items) {
			makeContributionVisible(item);
		}
	}

	/**
	 * Makes all items under the item to be visible, so they can be read.
	 *
	 * @param item
	 */
	private static void makeContributionVisible(IContributionItem item) {
		item.setVisible(true);

		if (item instanceof IContributionManager) {
			makeAllContributionsVisible((IContributionManager) item);
		}
		if (item instanceof SubContributionItem) {
			makeContributionVisible(((SubContributionItem) item).getInnerItem());
		}
	}

	private DisplayItem createMenuStructure(MMenu menu) {
		DisplayItem root = new DisplayItem("", null); //$NON-NLS-1$
		createMenuEntries(menu, root);
		return root;
	}

	private void createMenuEntries(MMenu menu, DisplayItem parent) {
		Map<IContributionItem, IContributionItem> findDynamics = new HashMap<>();
		DynamicContributionItem dynamicEntry = null;

		if (menu.getParent() != null) {
			// Search for any dynamic menu entries which will be handled later
			IContributionManager manager = menuMngrRenderer.getManager(menu);
			if (manager != null) {
				IContributionItem[] items = manager.getItems();
				for (int i = 0; i < items.length; i++) {
					IContributionItem ci = items[i];
					if (ci.isDynamic()) {
						findDynamics.put(i > 0 ? items[i - 1] : null, ci);
					}
				}
				// If there is an item with no preceding item, set it up to be
				// added first.
				if (findDynamics.containsKey(null)) {
					IContributionItem item = findDynamics.get(null);
					dynamicEntry = new DynamicContributionItem(item);
					dynamicEntry.setCheckState(getMenuItemIsVisible(dynamicEntry));
					dynamicEntry.setActionSet(idToActionSet.get(getActionSetID(item)));
					parent.addChild(dynamicEntry);
				}
			}
		}

		for (MMenuElement menuItem : menu.getChildren()) {
			dynamicEntry = createMenuEntry(parent, findDynamics, dynamicEntry, menuItem);
		}
	}

	private DynamicContributionItem createMenuEntry(DisplayItem parent,
			Map<IContributionItem, IContributionItem> findDynamics, DynamicContributionItem dynamicEntry,
			MMenuElement menuItem) {
		String text = menuItem.getLocalizedLabel();
		if (text == null || text.length() == 0) {
			text = menuItem.getLabel();
		}
		if ((text != null && text.length() != 0)
				|| (menuItem instanceof MHandledMenuItem) || menuItem.getWidget() != null) {
			IContributionItem contributionItem;
			if (menuItem instanceof MMenu) {
				contributionItem = menuMngrRenderer.getManager((MMenu) menuItem);
			} else {
				contributionItem = menuMngrRenderer.getContribution(menuItem);
			}
			if (contributionItem == null) {
				return dynamicEntry;
			}
			if (dynamicEntry != null
					&& contributionItem.equals(dynamicEntry.getIContributionItem())) {
				// If the last item added is the item meant to go before the
				// given dynamic entry, add the dynamic entry so it is in
				// the correct order.
				dynamicEntry.addCurrentItem((MenuItem) menuItem.getWidget());
				// TODO: might not work
			} else {
				ImageDescriptor iconDescriptor = null;
				String iconURI = menuItem.getIconURI();
				if (iconURI != null && iconURI.length() > 0) {
					iconDescriptor = resUtils.imageDescriptorFromURI(URI.createURI(iconURI));
				}

				if (menuItem.getWidget() instanceof MenuItem) {
					MenuItem item = (MenuItem) menuItem.getWidget();
					if (text == null) {
						if ("".equals(item.getText())) { //$NON-NLS-1$
							return dynamicEntry;
						}
						text = item.getText();
					}
					if (iconDescriptor == null) {
						Image image = item.getImage();
						if (image != null) {
							iconDescriptor = ImageDescriptor.createFromImage(image);
						}
					}
				} else if (menuItem instanceof MHandledMenuItem) {
					MHandledMenuItem hmi = (MHandledMenuItem) menuItem;
					final String i18nLabel = hmi.getLocalizedLabel();
					if (i18nLabel != null) {
						text = i18nLabel;
					} else if (hmi.getWbCommand() != null) {
						try {
							text = hmi.getWbCommand().getName();
						} catch (NotDefinedException e) {
							// we'll just ignore a failure
						}
					}
				}
				DisplayItem menuEntry = new DisplayItem(text, contributionItem);

				if (iconDescriptor != null) {
					menuEntry.setImageDescriptor(iconDescriptor);
				}
				menuEntry.setActionSet(idToActionSet.get(getActionSetID(menuItem)));
				parent.addChild(menuEntry);

				if (ActionFactory.NEW.getId().equals(contributionItem.getId())) {
					initializeNewWizardsMenu(menuEntry);
					wizards = menuEntry;
				} else if (SHORTCUT_CONTRIBUTION_ITEM_ID_OPEN_PERSPECTIVE
						.equals(contributionItem.getId())) {
					initializePerspectivesMenu(menuEntry);
					perspectives = menuEntry;
				} else if (SHORTCUT_CONTRIBUTION_ITEM_ID_SHOW_VIEW.equals(contributionItem
						.getId())) {
					initializeViewsMenu(menuEntry);
					views = menuEntry;
				} else {
					if (menuItem instanceof MMenu) {// TODO:menuItem any
													// other instance
						createMenuEntries((MMenu) menuItem, menuEntry);
					}
				}

				if (menuEntry.getChildren().isEmpty()) {
					menuEntry.setCheckState(getMenuItemIsVisible(menuEntry));
				}

				if (iconDescriptor == null) {
					if (parent.getParent() == null) {
						menuEntry.setImageDescriptor(menuImageDescriptor);
					} else if (menuEntry.getChildrenCount() > 0) {
						menuEntry.setImageDescriptor(submenuImageDescriptor);
					}
				}
			}
			if (findDynamics.containsKey(contributionItem)) {
				IContributionItem item = findDynamics.get(contributionItem);
				dynamicEntry = new DynamicContributionItem(item);
				dynamicEntry.setCheckState(getMenuItemIsVisible(dynamicEntry));
				dynamicEntry.setActionSet(idToActionSet.get(getActionSetID(contributionItem)));
				parent.addChild(dynamicEntry);
			} else {
				return dynamicEntry;
			}
		} else if (OpaqueElementUtil.isOpaqueMenuItem(menuItem)) {
			IContributionItem contributionItem = menuMngrRenderer.getContribution(menuItem);
			if (contributionItem instanceof ActionContributionItem) {
				final IAction action = ((ActionContributionItem) contributionItem).getAction();
				DisplayItem menuEntry = new DisplayItem(action.getText(), contributionItem);
				menuEntry.setImageDescriptor(action.getImageDescriptor());
				menuEntry.setActionSet(idToActionSet.get(getActionSetID(contributionItem)));
				parent.addChild(menuEntry);
				if (menuEntry.getChildren().isEmpty()) {
					menuEntry.setCheckState(getMenuItemIsVisible(menuEntry));
				}
			}
		} else {
			return dynamicEntry;
		}
		return dynamicEntry;
	}

	private boolean getMenuItemIsVisible(DisplayItem item) {
		return getItemIsVisible(item, ModeledPageLayout.HIDDEN_MENU_PREFIX);
	}

	private boolean getToolbarItemIsVisible(DisplayItem item) {
		return getItemIsVisible(item, ModeledPageLayout.HIDDEN_TOOLBAR_PREFIX);
	}

	private boolean getItemIsVisible(DisplayItem item, String prefix) {
		return isAvailable(item) && !isHiddenItem(item, prefix);
	}

	private boolean isHiddenItem(DisplayItem item, String prefix) {
		String itemId = prefix + getCommandID(item) + ","; //$NON-NLS-1$
		return windowPage.getHiddenItems().contains(itemId);
	}

	/**
	 * Causes a viewer to update the state of a category and all its ancestors.
	 *
	 * @param viewer
	 * @param category
	 */
	private void updateCategoryAndParents(StructuredViewer viewer,
			Category category) {
		while (category.getParent() != shortcuts) {
			viewer.update(category, null);
			category = (Category) category.getParent();
		}
	}

	private static boolean hasVisibleItems(MToolBar toolBar) {
		for (MToolBarElement e : toolBar.getChildren()) {
			if (!(e instanceof MToolBarSeparator)) {
				return true;
			}
		}
		return false;
	}

	private DisplayItem createTrimBarEntries(MTrimBar trimBar) {
		// create a root element
		DisplayItem root = new DisplayItem(null, null);
		if (trimBar == null) {
			return root;
		}
		for (MTrimElement trimElement : trimBar.getChildren()) {
			if (!(trimElement instanceof MToolBar)) {
				continue;
			}
			MToolBar toolBar = (MToolBar) trimElement;
			ToolBarManager manager = toolbarMngrRenderer.getManager(toolBar);
			if (manager != null) {
				toolbarMngrRenderer.reconcileManagerToModel(manager, toolBar);
				IContributionItem contributionItem = (IContributionItem) toolBar.getTransientData().get(
						CoolBarToTrimManager.OBJECT);
				String text = getToolbarLabel(toolBar);
				DisplayItem toolBarEntry = new DisplayItem(text, contributionItem);
				toolBarEntry.setImageDescriptor(toolbarImageDescriptor);
				toolBarEntry.setActionSet(idToActionSet.get(getActionSetID(toolBar)));
				if (!hasVisibleItems(toolBar)) {
					// TODO: there are two "Launch" toolbars, one of them is
					// empty. Why?
					continue;
				}
				root.addChild(toolBarEntry);
				toolBarEntry.setCheckState(getToolbarItemIsVisible(toolBarEntry));
				createToolbarEntries(toolBar, toolBarEntry);
			}
		}
		return root;
	}

	private void createToolbarEntries(MToolBar toolbar, DisplayItem parent) {
		if (toolbar == null) {
			return;
		}
		for (MToolBarElement element : toolbar.getChildren()) {
			createToolbarEntry(parent, element);
		}
	}

	private void createToolbarEntry(DisplayItem parent, MToolBarElement element) {
		IContributionItem contributionItem = toolbarMngrRenderer.getContribution(element);
		if (isGroupOrSeparator(element, contributionItem)) {
			return;
		}

		if (OpaqueElementUtil.isOpaqueToolItem(element)) {
			if (contributionItem instanceof ActionContributionItem) {
				final IAction action = ((ActionContributionItem) contributionItem).getAction();
				DisplayItem toolbarEntry = new DisplayItem(action.getText(), contributionItem);
				toolbarEntry.setImageDescriptor(action.getImageDescriptor());
				toolbarEntry.setActionSet(idToActionSet.get(getActionSetID(contributionItem)));
				if (toolbarEntry.getChildren().isEmpty()) {
					toolbarEntry.setCheckState(getToolbarItemIsVisible(toolbarEntry));
				}
				parent.addChild(toolbarEntry);
			}
			return;
		}

		String text = null;
		if (element instanceof MItem) {
			text = getToolTipText((MItem) element);
		}
		ImageDescriptor iconDescriptor = null;
		String iconURI = element instanceof MItem ? ((MItem) element).getIconURI() : null;
		if (iconURI != null && iconURI.length() > 0) {
			iconDescriptor = resUtils.imageDescriptorFromURI(URI.createURI(iconURI));
		}
		if (element.getWidget() instanceof ToolItem) {
			ToolItem item = (ToolItem) element.getWidget();
			if (text == null) {
				text = item.getToolTipText();
			}
			if (text == null) {
				text = item.getText();
			}
			if (iconDescriptor == null) {
				Image image = item.getImage();
				if (image != null) {
					iconDescriptor = ImageDescriptor.createFromImage(image);
				}
			}
		}
		if (text == null) {
			text = getToolbarLabel(element);
		}

		DisplayItem toolBarEntry = new DisplayItem(text, contributionItem);
		if (iconDescriptor != null) {
			toolBarEntry.setImageDescriptor(iconDescriptor);
		}
		toolBarEntry.setActionSet(idToActionSet.get(getActionSetID(element)));
		if (toolBarEntry.getChildren().isEmpty()) {
			toolBarEntry.setCheckState(getToolbarItemIsVisible(toolBarEntry));
		}
		parent.addChild(toolBarEntry);
	}

	private static boolean isGroupOrSeparator(MToolBarElement element, IContributionItem contributionItem) {
		return element instanceof MToolBarSeparator
				|| (contributionItem == null || contributionItem.isGroupMarker() || contributionItem
						.isSeparator());
	}

	private static ParameterizedCommand generateParameterizedCommand(final MHandledItem item,
			final IEclipseContext lclContext) {
		ECommandService cmdService = lclContext.get(ECommandService.class);
		Map<String, Object> parameters = null;
		List<MParameter> modelParms = item.getParameters();
		if (modelParms != null && !modelParms.isEmpty()) {
			parameters = new HashMap<>();
			for (MParameter mParm : modelParms) {
				parameters.put(mParm.getName(), mParm.getValue());
			}
		}
		ParameterizedCommand cmd = cmdService.createCommand(item.getCommand().getElementId(),
				parameters);
		item.setWbCommand(cmd);
		return cmd;
	}

	private String getToolTipText(MItem item) {
		String text = item.getLocalizedTooltip();
		if (item instanceof MHandledItem) {
			MHandledItem handledItem = (MHandledItem) item;
			EBindingService bs = context.get(EBindingService.class);
			ParameterizedCommand cmd = handledItem.getWbCommand();
			if (cmd == null) {
				cmd = generateParameterizedCommand(handledItem, context);
			}
			TriggerSequence sequence = bs.getBestSequenceFor(handledItem.getWbCommand());
			if (sequence != null) {
				if (text == null) {
					try {
						text = cmd.getName();
					} catch (NotDefinedException e) {
						return null;
					}
				}
				text = text + " (" + sequence.format() + ')'; //$NON-NLS-1$
			}
			return text;
		} else if (OpaqueElementUtil.isOpaqueMenuItem(item)) {
			Object opaque = OpaqueElementUtil.getOpaqueItem(item);
			if (opaque instanceof ActionContributionItem) {
				return ((ActionContributionItem) opaque).getAction().getText();
			}
		} else if (OpaqueElementUtil.isOpaqueToolItem(item)) {
			Object opaque = OpaqueElementUtil.getOpaqueItem(item);
			if (opaque instanceof ActionContributionItem) {
				return ((ActionContributionItem) opaque).getAction().getToolTipText();
			}
		}
		return text;
	}

	/**
	 * Returns whether the shortcut tab should be shown.
	 *
	 * @return <code>true</code> if the shortcut tab should be shown, and
	 *         <code>false</code> otherwise
	 * @since 3.0
	 */
	private boolean showShortcutTab() {
		return window.containsSubmenu(WorkbenchWindow.NEW_WIZARD_SUBMENU)
				|| window
						.containsSubmenu(WorkbenchWindow.OPEN_PERSPECTIVE_SUBMENU)
				|| window.containsSubmenu(WorkbenchWindow.SHOW_VIEW_SUBMENU);
	}

	private static ArrayList<String> getVisibleIDs(DisplayItem root) {
		if (root == null) {
			return new ArrayList<>();
		}
		ArrayList<String> ids = new ArrayList<>(root.getChildrenCount());
		for (TreeItem treeItem : root.getChildren()) {
			DisplayItem object = (DisplayItem) treeItem;
			if (object instanceof ShortcutItem && object.getState()) {
				ids.add(getParamID(object));
			}
		}
		return ids;
	}

	private void getChangedIds(DisplayItem item, List<String> invisible, List<String> visible) {
		if (item instanceof ShortcutItem) {
			return;
		}

		if (item == wizards || item == perspectives || item == views) {
			// We always want the top-level wizard/perspective/view shortcuts to
			// be visible, see bug 293448
			return;
		} else if (item.getChildrenCount() > 0) {
			if (item.isChangedByUser()) {
				String id = getCommandID(item);
				if (id != null) {
					if (item.getState()) {
						visible.add(id);
					} else {
						invisible.add(id);
					}
				}
			}
			for (TreeItem treeItem : item.getChildren()) {
				getChangedIds((DisplayItem) treeItem, invisible, visible);
			}
		} else if (item.isChangedByUser()) {
			String id = getCommandID(item);
			if (id != null) {
				if (item.getState()) {
					visible.add(id);
				} else {
					invisible.add(id);
				}
			}
		}
	}

	private boolean updateHiddenElements(List<ActionSet> items, String currentHidden, String prefix) {
		List<String> changedAndVisible = new ArrayList<>();
		List<String> changedAndInvisible = new ArrayList<>();
		for (ActionSet actionSet : items) {
			if (!actionSet.wasChanged()) {
				continue;
			}
			if (actionSet.isActive()) {
				changedAndVisible.add(actionSet.descriptor.getId());
			} else {
				changedAndInvisible.add(actionSet.descriptor.getId());
			}
		}
		return updateHiddenElements(currentHidden, prefix, changedAndVisible, changedAndInvisible);
	}

	private boolean updateHiddenElements(DisplayItem items, String currentHidden, String prefix) {
		List<String> changedAndVisible = new ArrayList<>();
		List<String> changedAndInvisible = new ArrayList<>();
		getChangedIds(items, changedAndInvisible, changedAndVisible);

		return updateHiddenElements(currentHidden, prefix, changedAndVisible, changedAndInvisible);
	}

	private boolean updateHiddenElements(String currentHidden, String prefix, List<String> changedAndVisible,
			List<String> changedAndInvisible) {
		boolean hasChanges = false;
		// Remove explicitly 'visible' elements from the current list
		for (String id : changedAndVisible) {
			String itemId = prefix + id;
			if (currentHidden.contains(itemId + ",")) { //$NON-NLS-1$
				hasChanges = true;
				windowPage.removeHiddenItems(itemId);
			}
		}

		// Add explicitly 'hidden' elements to the current list
		for (String id : changedAndInvisible) {
			String itemId = prefix + id;
			if (!currentHidden.contains(itemId + ",")) { //$NON-NLS-1$
				hasChanges = true;
				windowPage.addHiddenItems(itemId);
			}
		}

		return hasChanges;
	}

	@Override
	protected void okPressed() {

		// Shortcuts
		if (showShortcutTab()) {
			windowPage.setNewShortcuts(getVisibleIDs(wizards), ModeledPageLayout.NEW_WIZARD_TAG);
			windowPage.setNewShortcuts(getVisibleIDs(perspectives), ModeledPageLayout.PERSP_SHORTCUT_TAG);
			windowPage.setNewShortcuts(getVisibleIDs(views), ModeledPageLayout.SHOW_VIEW_TAG);
		}

		// Determine if anything has changed and, if so, update the menu & tb's
		boolean requiresUpdate = false;

		// Action Sets
		ArrayList<ActionSetDescriptor> toAdd = new ArrayList<>();
		ArrayList<ActionSetDescriptor> toRemove = new ArrayList<>();

		for (ActionSet actionSet : actionSets) {
			if (!actionSet.wasChanged()) {
				continue;
			}

			// Something has changed
			requiresUpdate = true;

			if (actionSet.isActive()) {
				toAdd.add(actionSet.descriptor);
			} else {
				toRemove.add(actionSet.descriptor);
			}
		}

		perspective.turnOnActionSets(toAdd.toArray(new IActionSetDescriptor[toAdd.size()]));
		perspective.turnOffActionSets(toRemove.toArray(new IActionSetDescriptor[toRemove.size()]));

		requiresUpdate |= updateHiddenElements(actionSets, windowPage.getHiddenItems(),
				ModeledPageLayout.HIDDEN_ACTIONSET_PREFIX);
		// Menu and Toolbar Items
		requiresUpdate |= updateHiddenElements(menuItems, windowPage.getHiddenItems(),
				ModeledPageLayout.HIDDEN_MENU_PREFIX);
		requiresUpdate |= updateHiddenElements(toolBarItems, windowPage.getHiddenItems(),
				ModeledPageLayout.HIDDEN_TOOLBAR_PREFIX);

		if (requiresUpdate) {
			perspective.updateActionBars();
		}

		super.okPressed();
	}

	@Override
	public boolean close() {

		treeManager.dispose();
		customizeActionBars.dispose();

		return super.close();
	}

	private static String removeShortcut(String label) {
		if (label == null) {
			return label;
		}
		int end = label.lastIndexOf('@');
		if (end >= 0) {
			label = label.substring(0, end);
		}

		end = label.lastIndexOf('\t');
		if (end >= 0) {
			label = label.substring(0, end);
		}

		return label;
	}


	@Override
	protected boolean isResizable() {
		return true;
	}

	void showActionSet(final DisplayItem item) {
		if (item.getActionSet() != null) {
			showActionSet(item.getActionSet());
		}
	}

	void showActionSet(final ActionSet actionSet) {
		tabFolder.setSelection(actionSetTab);
		actionSetAvailabilityTable.reveal(actionSet);
		setSelectionOn(actionSetAvailabilityTable, actionSet);
		actionSetAvailabilityTable.getControl().setFocus();
	}

}
