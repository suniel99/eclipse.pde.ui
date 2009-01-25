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

import org.eclipse.osgi.util.NLS;

/**
 * All externalized messages found in the ant tasks.
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.tasks.messages"; //$NON-NLS-1$

	public static String deltaReportTask_footer;
	public static String deltaReportTask_header;
	public static String deltaReportTask_entry;
	public static String deltaReportTask_componentEntry;
	public static String deltaReportTask_endComponentEntry;
	public static String deltaReportTask_entry_major_version;
	public static String deltaReportTask_entry_minor_version;
	public static String deltaReportTask_missingXmlFileLocation;
	public static String deltaReportTask_couldNotCreateSAXParser;

	public static String fullReportTask_bundlesheader;
	public static String fullReportTask_bundlesentry_even;
	public static String fullReportTask_bundlesentry_odd;
	public static String fullReportTask_bundlesfooter;

	public static String fullReportTask_apiproblemfooter;
	public static String fullReportTask_apiproblemheader;
	public static String fullReportTask_apiproblemsummary;

	public static String fullReportTask_categoryheader;
	public static String fullReportTask_categoryfooter;
	public static String fullReportTask_problementry_even_error;
	public static String fullReportTask_problementry_even_warning;
	public static String fullReportTask_problementry_odd_error;
	public static String fullReportTask_problementry_odd_warning;
	public static String fullReportTask_categoryseparator;
	public static String fullReportTask_category_no_elements;
	
	public static String fullReportTask_indexheader;
	public static String fullReportTask_indexfooter;
	public static String fullReportTask_indexsummary_even;
	public static String fullReportTask_indexsummary_odd;
	
	public static String fullReportTask_missingXmlFilesLocation;
	public static String fullReportTask_invalidDirectoryName;
	public static String fullReportTask_couldNotCreateSAXParser;
	public static String fullReportTask_couldNotCreateFile;
	public static String fullReportTask_nonApiBundleSummary;
	public static String fullReportTask_apiBundleSummary;
	public static String fullReportTask_ioexceptionDumpingHtmlFile;
	public static String fullReportTask_compatibility_header;
	public static String fullReportTask_api_usage_header;
	public static String fullReportTask_bundle_version_header;

	public static String printArguments;
	public static String errorInComparison;
	public static String errorCreatingParentReportFile;
	public static String errorCreatingReportDirectory;
	public static String directoryIsEmpty;
	public static String fileDoesnotExist;
	public static String couldNotDelete;
	public static String couldNotCreate;
	public static String couldNotUnzip;
	public static String couldNotUntar;

	public static String api_generation_printArguments;
	public static String api_generation_printArguments2;
	public static String api_generation_projectLocationNotADirectory;
	public static String api_generation_targetFolderNotADirectory;
	public static String api_generation_invalidBinaryLocation;
	
	public static String comparison_invalidRegularExpression;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}