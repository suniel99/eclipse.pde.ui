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
package org.eclipse.pde.api.tools.internal.problems;

import org.eclipse.core.runtime.Assert;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * Base implementation of {@link IApiProblemFilter}
 * 
 * @since 1.0.0
 */
public class ApiProblemFilter implements IApiProblemFilter, Cloneable {

	private String fComponentId = null;
	private IApiProblem fProblem = null;
	
	/**
	 * Constructor
	 * 
	 * @param componentid
	 * @param problem
	 */
	public ApiProblemFilter(String componentid, IApiProblem problem) {
		fComponentId = componentid;
		Assert.isNotNull(problem);
		fProblem = problem;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProblemFilter#getComponentId()
	 */
	public String getComponentId() {
		return fComponentId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IApiProblemFilter) {
			IApiProblemFilter filter = (IApiProblemFilter) obj;
			return elementsEqual(filter.getComponentId(), fComponentId) &&
					filter.getUnderlyingProblem().equals(fProblem);
		}
		else if(obj instanceof IApiProblem) {
			return fProblem.equals(obj);
		}
		return super.equals(obj);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fProblem.hashCode() + fComponentId.hashCode();
	}
	
	/**
	 * Returns if the two specified objects are equal.
	 * Objects are considered equal if:
	 * <ol>
	 * <li>they are both null</li>
	 * <li>they are equal via the default .equals() method</li>
	 * </ol>
	 * @param s1
	 * @param s2
	 * @return true if the objects are equal, false otherwise
	 */
	private boolean elementsEqual(Object s1, Object s2) {
		return (s1 == null && s2 == null) || (s1 != null && s1.equals(s2));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Filter for : "); //$NON-NLS-1$
		buffer.append(fProblem.toString());
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		return new ApiProblemFilter(this.fComponentId, fProblem);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter#getUnderlyingProblem()
	 */
	public IApiProblem getUnderlyingProblem() {
		return fProblem;
	}
	
	/**
	 * @return returns a handle that can be used to identify the filter
	 */
	public String getHandle() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(fProblem.getId());
		buffer.append("%]"); //$NON-NLS-1$
		buffer.append(fProblem.getResourcePath());
		buffer.append("%]"); //$NON-NLS-1$
		buffer.append(fProblem.getTypeName());
		buffer.append("%]"); //$NON-NLS-1$
		String[] margs = fProblem.getMessageArguments();
		for(int i = 0; i < margs.length; i++) {
			buffer.append(margs[i]);
			if(i < margs.length-1) {
				buffer.append(',');
			}
		}
		return buffer.toString();
	}
}