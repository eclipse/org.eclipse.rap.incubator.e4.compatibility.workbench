/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.ui.internal.wizards;

import org.eclipse.rap.rwt.SingletonUtil;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;

/**
 * Registry that contains wizards contributed via the <code>newWizards</code>
 * extension point.
 *
 * @since 3.1
 */
public final class NewWizardRegistry extends AbstractExtensionWizardRegistry {

// RAP [rst] session singleton
//		private static NewWizardRegistry singleton;

	/**
	 * Return the singleton instance of this class.
	 * 
	 * @return the singleton instance of this class
	 */
	public static synchronized NewWizardRegistry getInstance() {
		// RAP [rst] session singleton
		return SingletonUtil.getSessionInstance(NewWizardRegistry.class);
//			if (singleton == null) {
//				singleton = new NewWizardRegistry();
//			}
//			return singleton;
		}

	/**
	 * Private constructor.
	 */
	private NewWizardRegistry() {
		super();
	}

	@Override
	protected String getExtensionPoint() {
		return IWorkbenchRegistryConstants.PL_NEW;
	}

	@Override
	protected String getPlugin() {
        // RAP [bm]: namespace
//      return PlatformUI.PLUGIN_ID;
        return PlatformUI.PLUGIN_EXTENSION_NAME_SPACE;
        // RAPEND: [bm]
	}
}
