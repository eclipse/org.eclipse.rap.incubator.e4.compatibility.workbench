/**********************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 * Lucas Bullen (Red Hat Inc.) - Bug 525343: importPreferencesremoves preferenceChangedListeners
 **********************************************************************/
package org.eclipse.ui.internal.wizards.preferences;

import org.eclipse.rap.rwt.RWT;


/**
 * NLS messages class for preferences messages.

 * @since 3.1
 *
 */
//RAP [if]: need session aware NLS
//public class PreferencesMessages extends NLS {
public class PreferencesMessages {
	private static final String BUNDLE_NAME = "org.eclipse.ui.internal.wizards.preferences.messages";//$NON-NLS-1$

//	public static String WizardPreferences_description;
//	public static String WizardPreferencesPage_noOptionsSelected;
//	public static String WizardPreferences_noSpecificPreferenceDescription;
//
//	public static String PreferencesExportWizard_export;
//	public static String WizardPreferencesExportPage1_exportTitle;
//	public static String WizardPreferencesExportPage1_exportDescription;
//	public static String WizardPreferencesExportPage1_preferences;
//	public static String WizardPreferencesExportPage1_noPrefFile;
//	public static String WizardPreferencesExportPage1_overwrite;
//	public static String WizardPreferencesExportPage1_title;
//	public static String WizardPreferencesExportPage1_all;
//	public static String WizardPreferencesExportPage1_choose;
//	public static String WizardPreferencesExportPage1_file;
//
//	public static String PreferencesExport_error;
//	public static String PreferencesExport_browse;
//	public static String PreferencesExport_createTargetDirectory;
//	public static String PreferencesExport_directoryCreationError;
//	public static String ExportFile_overwriteExisting;
//
//	public static String PreferencesImportWizard_import;
//	public static String WizardPreferencesImportPage1_importTitle;
//	public static String WizardPreferencesImportPage1_importDescription;
//	public static String WizardPreferencesImportPage1_all;
//	public static String WizardPreferencesImportPage1_choose;
//	public static String WizardPreferencesImportPage1_file;
//	public static String WizardPreferencesImportPage1_title;
//	public static String WizardPreferencesImportPage1_invalidPrefFile;
//	public static String WizardPreferencesImportRestartDialog_title;
//	public static String WizardPreferencesImportRestartDialog_message;
//	public static String WizardPreferencesImportRestartDialog_restart;
//
//	public static String SelectionDialog_selectLabel;
//	public static String SelectionDialog_deselectLabel;
//
//
//	public static String WizardDataTransfer_existsQuestion;
//	public static String WizardDataTransfer_overwriteNameAndPathQuestion;
//	public static String Question;

	public String WizardPreferences_description;
	public String WizardPreferencesPage_noOptionsSelected;
	public String WizardPreferences_noSpecificPreferenceDescription;

	public String PreferencesExportWizard_export;
	public String WizardPreferencesExportPage1_exportTitle;
	public String WizardPreferencesExportPage1_exportDescription;
	public String WizardPreferencesExportPage1_preferences;
	public String WizardPreferencesExportPage1_noPrefFile;
	public String WizardPreferencesExportPage1_overwrite;
	public String WizardPreferencesExportPage1_title;
	public String WizardPreferencesExportPage1_all;
	public String WizardPreferencesExportPage1_choose;
	public String WizardPreferencesExportPage1_file;

	public String PreferencesExport_error;
	public String PreferencesExport_browse;
	public String PreferencesExport_createTargetDirectory;
	public String PreferencesExport_directoryCreationError;
	public String ExportFile_overwriteExisting;

	public String PreferencesImportWizard_import;
	public String WizardPreferencesImportPage1_importTitle;
	public String WizardPreferencesImportPage1_importDescription;
	public String WizardPreferencesImportPage1_all;
	public String WizardPreferencesImportPage1_choose;
	public String WizardPreferencesImportPage1_file;
	public String WizardPreferencesImportPage1_title;
	public String WizardPreferencesImportPage1_invalidPrefFile;
	public String WizardPreferencesImportRestartDialog_title;
	public String WizardPreferencesImportRestartDialog_message;
	public String WizardPreferencesImportRestartDialog_restart;

	public String SelectionDialog_selectLabel;
	public String SelectionDialog_deselectLabel;


	public String WizardDataTransfer_existsQuestion;
	public String WizardDataTransfer_overwriteNameAndPathQuestion;
	public String Question;

// RAP [if]: need session aware NLS
//		static {
//			// load message values from bundle file
//			NLS.initializeMessages(BUNDLE_NAME, PreferencesMessages.class);
//		}

	/**
	 * Load message values from bundle file
	 * 
	 * @return localized message
	 */
	public static PreferencesMessages get() {
		return RWT.NLS.getISO8859_1Encoded(BUNDLE_NAME, PreferencesMessages.class);
	}
}