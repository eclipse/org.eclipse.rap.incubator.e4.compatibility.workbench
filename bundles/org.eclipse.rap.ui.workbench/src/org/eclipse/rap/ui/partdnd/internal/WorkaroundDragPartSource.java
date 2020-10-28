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

import static org.eclipse.rap.rwt.internal.protocol.RemoteObjectFactory.getRemoteObject;
import static org.eclipse.swt.internal.dnd.DNDUtil.convertTransferTypes;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.rap.json.JsonValue;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;


/**
 * This class is implemented as a workaround for the following issue:<br>
 * <br>
 * When the window is resized and the mouse is on the border of the part (not the part tab itself),
 * the CTabFolder is catching the event and corrupting the browser's window widget by sending a
 * wrong JSON message. After this occurs, the window becomes frozen and it is not possible to
 * restore its functionality. <br>
 * In order to avoid this situation, a mouse listener will decide if the widget under the cursor
 * really belongs to the CTabFolder (ignoring the part's borders).It is important to note that the
 * event sequence might cause that the DragSource event is triggered before the MouseDown event is
 * sent. If that happens, the next mouse events in a short period of time have to be ignored.
 * 
 * @author David Marina
 */
public class WorkaroundDragPartSource extends DragSource {

  /** The serialVersionUID of this WorkaroundDragPartSource.java */
  private static final long serialVersionUID = 8801486978319382719L;
  /** The reference to a empty Transfer array */
  private static final Transfer[] EMPTY_TRANSFER = new Transfer[] {};
  /** The reference to the {@link CTabFolder} */
  private final CTabFolder ctf;
  /** Flag to indicated that the drag has started (<code>true</code>) */
  private boolean dragStarted = false;
  /**
   * Flag to ignore mouse events (with <code>true</code>) when the DragStar happens before the
   * MouseDown
   */
  private boolean ignoreEvents = false;

  /**
   * The reference to the thread pool executor
   */
  private ScheduledThreadPoolExecutor executor;
  
  /**
   * Creates a new WorkaroundDragPartSource
   * 
   * @param control
   * @param style
   */
  public WorkaroundDragPartSource( Control control, int style ) {
    super( control, style );
    this.ctf = ( CTabFolder )control;
    Listener listener = new Listener() {

      /** The serialVersionUID of this WorkaroundDragPartSource.java */
      private static final long serialVersionUID = 7747075970781676002L;

      /** {@inheritDoc} */
      @Override
      public void handleEvent( org.eclipse.swt.widgets.Event event ) {
        if( event.type == SWT.MouseDown ) {
          Point absolutePoint = new Point( event.x, event.y );
          CTabItem item = WorkaroundDragPartSource.this.ctf.getItem( absolutePoint );
          if( item == null ) {
            setDragStarted( false );
          }
          // The mouse down event could arrive after the DragStart. In
          // that case we should ignore the mouse event
          else if( !WorkaroundDragPartSource.this.ignoreEvents ) {
            setDragStarted( true );
            startClientTransferRendering();
          }
        } else if( event.type == SWT.MouseUp ) {
          setDragStarted( false );
        }
      }
    };
    if( this.ctf.getData( "DragWorkaroundListener" ) == null ) {
      this.ctf.addListener( SWT.MouseDown, listener );
      this.ctf.addListener( SWT.MouseUp, listener );
      this.ctf.setData( "DragWorkaroundListener", listener );
    }

    this.executor = new ScheduledThreadPoolExecutor( 1 );
    
    this.ctf.getDisplay().disposeExec(() -> { this.executor.shutdown(); this.executor = null; });
  }

  /**
   * Indicates with an argument that the drag started (<code>true</code>) or ended (
   * <code>false</code>)
   * 
   * @param dragStarted a boolean indicating with <code>true</code> the drag start;
   *          <code>false</code> to indicate the drag end
   */
  public void setDragStarted( boolean dragStarted ) {
    // If there is no drag operation, set the empty transfer to avoid
    // blocking the window
    if( !dragStarted ) {
      JsonValue renderValue = convertTransferTypes( EMPTY_TRANSFER );
      getRemoteObject( this ).set( "transfer", renderValue );
      this.ignoreEvents = false;
    }
    this.dragStarted = dragStarted;
  }

  /** {@inheritDoc} */
  @Override
  public Transfer[] getTransfer() {
    Display display = getDisplay();
    Point location = display.getCursorLocation();
    Point relativePoint = display.map( null, this.ctf, location );
    CTabItem item = this.ctf.getItem( relativePoint );
    if( this.dragStarted || item != null ) {
      return super.getTransfer();
    } else {
      return EMPTY_TRANSFER;
    }
  }

  /** {@inheritDoc} */
  @Override
  public void notifyListeners( int eventType, Event event ) {
    if( eventType == DND.DragStart && !this.dragStarted ) {
      ignoreEvents();
    }
    super.notifyListeners( eventType, event );
    if( event.doit == false || event.type == DND.DragEnd ) {
      setDragStarted( false );
      ignoreEvents();
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void checkSubclass() {
    // Override to enable subclassing
  }

  /**
   * Starts the client transfer rendering
   */
  public void startClientTransferRendering() {
    JsonValue renderValue = convertTransferTypes( super.getTransfer() );
    getRemoteObject( this ).set( "transfer", renderValue );
  }

  /**
   * Indicates that the next mouse events shall be ignored for a short period of time
   */
  public void ignoreEvents() {
    this.ignoreEvents = true;
    
    this.executor.schedule( ( ) -> {
      this.ignoreEvents = false;
    }, 500, TimeUnit.MILLISECONDS );
  }
}
// -----------------------------------------------------------------------------
