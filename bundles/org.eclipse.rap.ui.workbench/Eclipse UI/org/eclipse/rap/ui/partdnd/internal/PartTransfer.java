/*******************************************************************************
 * Copyright (c) 2017 David Marina
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   David Marina - initial API and implementation
 *******************************************************************************/
package org.eclipse.rap.ui.partdnd.internal;

import org.eclipse.rap.rwt.SingletonUtil;


/**
 * @author David Marina
 */
public class PartTransfer extends SimpleObjectTransfer {

  public static final PartTransfer INSTANCE = SingletonUtil.getSessionInstance( PartTransfer.class );
  /** The serialVersionUID of this PartTransfer.java */
  private static final long serialVersionUID = 5415453120868372411L;
  private static final String TYPE_NAME = "org.eclipse.rap.ui.partdnd.internal.PartTransfer"
                                          + System.currentTimeMillis()
                                          + ":" + INSTANCE.hashCode(); //$NON-NLS-1$
  private static final int TYPE_ID = registerType( TYPE_NAME );

  /**
   * Creates a new PartTransfer
   */
  private PartTransfer() {
    // do nothing
  }

  /** {@inheritDoc} */
  @Override
  protected int[] getTypeIds() {
    return new int[] {
      TYPE_ID
    };
  }

  /** {@inheritDoc} */
  @Override
  protected String[] getTypeNames() {
    return new String[] {
      TYPE_NAME
    };
  }
}
// -----------------------------------------------------------------------------
