/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
 *     Daniel Kruegler <daniel.kruegler@gmail.com> - Bug 471310
 ******************************************************************************/

package org.eclipse.ui.internal;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.progress.ProgressRegion;

/**
 * @since 3.5
 *
 */
public class StandardTrim {
	@Inject
	EModelService modelService;

	private StatusLineManager manager;

	@PostConstruct
	void createWidget(Composite parent, MToolControl toolControl) {
		if (toolControl.getElementId().equals("org.eclipse.ui.StatusLine")) { //$NON-NLS-1$
			createStatusLine(parent, toolControl);
		} else if (toolControl.getElementId().equals("org.eclipse.ui.HeapStatus")) { //$NON-NLS-1$
			createHeapStatus(parent, toolControl);
		} else if (toolControl.getElementId().equals("org.eclipse.ui.ProgressBar")) { //$NON-NLS-1$
			createProgressBar(parent, toolControl);
		}
	}

	@PreDestroy
	void destroy() {
		if (manager != null) {
			manager.dispose();
			manager.removeAll();
			manager = null;
		}
	}

	/**
	 * @param parent
	 * @param toolControl
	 */
	private void createProgressBar(Composite parent, MToolControl toolControl) {
		IEclipseContext context = modelService.getContainingContext(toolControl);
		IEclipseContext child = context.createChild(ProgressRegion.class.getName());
		child.set(MToolControl.class, toolControl);
		child.set(Composite.class, parent);
		ContextInjectionFactory.make(ProgressRegion.class, child);
	}

	/**
	 * @param parent
	 * @param toolControl
	 */
	private void createHeapStatus(Composite parent, MToolControl toolControl) {
		// RAP has no JVM heap-status widget: the JVM is shared by all UI sessions.
	}

	/**
	 * @param parent
	 * @param toolControl
	 */
	private void createStatusLine(Composite parent, MToolControl toolControl) {
		IEclipseContext context = modelService.getContainingContext(toolControl);
		WorkbenchWindow wbw = (WorkbenchWindow) context.get(IWorkbenchWindow.class);
		// wbw may be null if workspace is started with no open perspectives.
		if (wbw == null) {
			// Create one assuming there's no defined perspective
			Workbench wb = (Workbench) PlatformUI.getWorkbench();
			wb.createWorkbenchWindow(wb.getDefaultPageInput(), null,
					modelService.getTopLevelWindowFor(toolControl), false);
			wbw = (WorkbenchWindow) context.get(IWorkbenchWindow.class);
		}

		if (wbw != null) {
			Workbench wb = (Workbench) PlatformUI.getWorkbench();
			wb.createWorkbenchWindow(wb.getDefaultPageInput(), null,
					modelService.getTopLevelWindowFor(toolControl), false);
			wbw = (WorkbenchWindow) context.get(IWorkbenchWindow.class);

			manager = wbw.getStatusLineManager();
			manager.createControl(parent);
		}
	}
}

