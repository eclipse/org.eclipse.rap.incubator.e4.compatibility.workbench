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
package org.eclipse.rap.ui.partdnd;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.rap.rwt.SingletonUtil;
import org.eclipse.rap.ui.partdnd.internal.PartTransfer;
import org.eclipse.rap.ui.partdnd.internal.WorkaroundDragPartSource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityPart;
import org.eclipse.ui.views.IViewDescriptor;


/**
 * Listener for setting drag and drop capabilities to the Compatibility Parts, which allows to
 * rearrange the parts in the workbench
 * 
 * @author David Marina
 */
public class DetachPartProvider implements IPartListener2 {

  /** The reference to the user singleton for this listener */
  public static final DetachPartProvider INSTANCE = SingletonUtil.getSessionInstance( DetachPartProvider.class );
  /** Tag for shell_detach_parts_menu */
  private static final String SHELL_DETACH_PARTS_MENU = "shell_detach_parts_menu"; //$NON-NLS-1$
  /** Tag for stack_selected_part */
  private static final String STACK_SELECTED_PART = "stack_selected_part"; //$NON-NLS-1$
  /** The reference to the model service */
  private EModelService modelService = null;

  /**
   * Creates a new DetachPartProvider
   */
  private DetachPartProvider() {
  }

  /**
   * Applies this listener to the given workbench page
   * 
   * @param page the reference to the workbench page where this listener will be applied
   */
  public static void applyListenerTo( IWorkbenchPage page ) {
    page.addPartListener( INSTANCE );
  }

  /** {@inheritDoc} */
  @Override
  public void partActivated( IWorkbenchPartReference partRef ) {
    // Add detach listener to tab item if not present yet
    applyDetachPartListeners( ( ( WorkbenchPartReference )partRef ).getModel() );
  }

  /** {@inheritDoc} */
  @Override
  public void partBroughtToTop( IWorkbenchPartReference partRef ) {
    // do nothing
  }

  /** {@inheritDoc} */
  @Override
  public void partClosed( IWorkbenchPartReference partRef ) {
    // do nothing
  }

  /** {@inheritDoc} */
  @Override
  public void partDeactivated( IWorkbenchPartReference partRef ) {
    // do nothing
  }

  /** {@inheritDoc} */
  @Override
  public void partOpened( IWorkbenchPartReference partRef ) {
    // do nothing
  }

  /** {@inheritDoc} */
  @Override
  public void partHidden( IWorkbenchPartReference partRef ) {
    // do nothing
  }

  /** {@inheritDoc} */
  @Override
  public void partVisible( IWorkbenchPartReference partRef ) {
    // Add detach listener to tab item if not present yet
    applyDetachPartListeners( ( ( WorkbenchPartReference )partRef ).getModel() );
  }

  /** {@inheritDoc} */
  @Override
  public void partInputChanged( IWorkbenchPartReference partRef ) {
    // do nothing
  }

  /**
   * Attaches drag and drop listeners to the CTabFolder that is the parent of the given Part, such
   * as the Parts can be rearranged into different locations of the application.
   * 
   * @param part the reference to the part that will attach the drag and drop listeners
   */
  private void applyDetachPartListeners( MPart part ) {
    Object client = part.getObject();
    if( !( client instanceof CompatibilityPart ) ) {
      String msg = "Tried to apply DetachPartProvider on a part that is not a Compatibility Part.";
      System.err.println( msg );
      return;
    }
    Composite parentComposite = part.getContext().get( Composite.class );
    Composite comp = parentComposite.getParent();
    while( comp != null && !( comp instanceof CTabFolder ) ) {
      comp = comp.getParent();
    }
    if( comp == null ) {
      return;
    }
    CTabFolder ctf = ( CTabFolder )comp;
    // Avoid setting multiple times the drag functionality
    if( ctf.getData( DND.DRAG_SOURCE_KEY ) != null ) {
      return;
    }
    this.modelService = part.getContext().get( EModelService.class );
    DragSource ds = new WorkaroundDragPartSource( ctf, DND.DROP_COPY | DND.DROP_MOVE );
    ds.setTransfer( new Transfer[] {
      PartTransfer.INSTANCE
    } );
    ds.addDragListener( new DragSourceListener() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = -3293045502426919932L;
      private Point dragStartCoordinates = null;

      /** {@inheritDoc} */
      @Override
      public void dragStart( DragSourceEvent event ) {
        Point absolutePoint = new Point( event.x, event.y );
        this.dragStartCoordinates = absolutePoint;
        CTabItem item = ctf.getItem( absolutePoint );
        if( item == null || item != null && !item.getShowClose() ) {
          event.doit = false;
        }
      }

      /** {@inheritDoc} */
      @Override
      public void dragSetData( DragSourceEvent event ) {
        CTabItem eventTabItem = ctf.getItem( this.dragStartCoordinates );
        if( eventTabItem != null ) {
          MUIElement uiElement = ( MUIElement )eventTabItem.getData( "modelElement" );
          MPart tabPart = ( MPart )( ( uiElement instanceof MPart )
                                                                   ? uiElement
                                                                   : ( ( MPlaceholder )uiElement ).getRef() );
          event.data = tabPart;
        }
      }

      /** {@inheritDoc} */
      @Override
      public void dragFinished( DragSourceEvent event ) {
        // Do nothing
      }
    } );
    DropTarget dt = new DropTarget( ctf, DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_MOVE );
    dt.setTransfer( new Transfer[] {
      PartTransfer.INSTANCE
    } );
    dt.addDropListener( new DropTargetListener() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = -574998030394417101L;

      /** {@inheritDoc} */
      @Override
      public void dropAccept( DropTargetEvent event ) {
        // Do nothing
      }

      /** {@inheritDoc} */
      @Override
      public void drop( DropTargetEvent event ) {
        Point absolutePoint = new Point( event.x, event.y );
        Point relativePoint = ctf.getDisplay().map( null, ctf, absolutePoint );
        CTabItem eventTabItem = ctf.getItem( relativePoint );
        if( eventTabItem == null ) {
          eventTabItem = ctf.getSelection();
          MUIElement uiElement = ( MUIElement )eventTabItem.getData( "modelElement" );
          MPart tabPart = ( MPart )( ( uiElement instanceof MPart )
                                                                   ? uiElement
                                                                   : ( ( MPlaceholder )uiElement ).getRef() );
          moveToPartStack( ( MPart )event.data, tabPart );
        } else {
          MUIElement uiElement = ( MUIElement )eventTabItem.getData( "modelElement" );
          MPart tabPart = ( MPart )( ( uiElement instanceof MPart )
                                                                   ? uiElement
                                                                   : ( ( MPlaceholder )uiElement ).getRef() );
          createMovePartContextMenu( absolutePoint, ctf, ( MPart )event.data, tabPart );
          // movePart((MPart)event.data, tabPart);
        }
      }

      /** {@inheritDoc} */
      @Override
      public void dragOver( DropTargetEvent event ) {
        // Do nothing
      }

      /** {@inheritDoc} */
      @Override
      public void dragOperationChanged( DropTargetEvent event ) {
        // Do nothing
      }

      /** {@inheritDoc} */
      @Override
      public void dragLeave( DropTargetEvent event ) {
        // Do nothing
      }

      /** {@inheritDoc} */
      @Override
      public void dragEnter( DropTargetEvent event ) {
        // Do nothing
      }
    } );
  }

  /**
   * Creates and shows the menu with all the detach options at the given location
   * 
   * @param location the menu location
   * @param control the control owner of the menu
   * @param partToBeMoved the part that is to be moved
   * @param destinationRefPart the reference part for placing the dragged part
   */
  private void createMovePartContextMenu( Point location,
                                          Control control,
                                          MPart partToBeMoved,
                                          MPart destinationRefPart )
  {
    Menu menu = createDetachMenu( control, partToBeMoved );
    menu.setData( STACK_SELECTED_PART, destinationRefPart );
    menu.setLocation( location );
    menu.setVisible( true );
  }

  /**
   * Creates and fills the part detach menu
   * 
   * @param control the reference to the control that will own the menu
   * @param partToBeMoved the reference to the part to be moved
   * @return the reference to the detach menu filled with all the menu items
   */
  private Menu createDetachMenu( Control control, MPart partToBeMoved ) {
    Shell shell = control.getShell();
    Menu cachedMenu = ( Menu )shell.getData( SHELL_DETACH_PARTS_MENU );
    if( cachedMenu == null ) {
      cachedMenu = new Menu( control );
      shell.setData( SHELL_DETACH_PARTS_MENU, cachedMenu );
    } else {
      for( MenuItem item : cachedMenu.getItems() ) {
        item.dispose();
      }
    }
    final Menu menu = cachedMenu;
    populateDetachMenu( menu, partToBeMoved );
    return menu;
  }

  /**
   * Populate the tab's context menu for the given part.
   *
   * @param menu the menu to be populated
   * @param partToBeMoved the relevant part
   */
  private void populateDetachMenu( final Menu menu, MPart partToBeMoved ) {
    // Insert into Part Stack option
    MenuItem menuItemAddToStack = new MenuItem( menu, SWT.NONE );
    menuItemAddToStack.setText( "Insert into Part Stack" );
    menuItemAddToStack.addSelectionListener( new SelectionAdapter() {

      /**
                 *
                 */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        moveToPartStack( partToBeMoved, part );
      }
    } );
    // Move Beside this Part options
    MenuItem moveItem = new MenuItem( menu, SWT.CASCADE );
    moveItem.setText( "Move Beside this Part" );
    Menu subMenu = new Menu( menu );
    moveItem.setMenu( subMenu );
    MenuItem menuItemMoveAbovePart = new MenuItem( subMenu, SWT.NONE );
    menuItemMoveAbovePart.setText( "Above" );
    menuItemMoveAbovePart.addSelectionListener( new SelectionAdapter() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        moveBeside( partToBeMoved, part, EModelService.ABOVE );
      }
    } );
    MenuItem menuItemMoveBelowPart = new MenuItem( subMenu, SWT.NONE );
    menuItemMoveBelowPart.setText( "Below" );
    menuItemMoveBelowPart.addSelectionListener( new SelectionAdapter() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        moveBeside( partToBeMoved, part, EModelService.BELOW );
      }
    } );
    MenuItem menuItemMoveLeftOfPart = new MenuItem( subMenu, SWT.NONE );
    menuItemMoveLeftOfPart.setText( "to the Left" );
    menuItemMoveLeftOfPart.addSelectionListener( new SelectionAdapter() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        moveBeside( partToBeMoved, part, EModelService.LEFT_OF );
      }
    } );
    MenuItem menuItemMoveRightOfPart = new MenuItem( subMenu, SWT.NONE );
    menuItemMoveRightOfPart.setText( "to the Right" );
    menuItemMoveRightOfPart.addSelectionListener( new SelectionAdapter() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        moveBeside( partToBeMoved, part, EModelService.RIGHT_OF );
      }
    } );
    // Move to the End options
    MenuItem moveToTheEndItem = new MenuItem( menu, SWT.CASCADE );
    moveToTheEndItem.setText( "Move to the End" );
    Menu subMenu2 = new Menu( menu );
    moveToTheEndItem.setMenu( subMenu2 );
    MenuItem menuItemMoveToTheEndAbove = new MenuItem( subMenu2, SWT.NONE );
    menuItemMoveToTheEndAbove.setText( "Above" );
    menuItemMoveToTheEndAbove.addSelectionListener( new SelectionAdapter() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        MWindow destWindow = part.getContext().get( MWindow.class );
        moveToTheEnd( partToBeMoved, destWindow, EModelService.ABOVE );
      }
    } );
    MenuItem menuItemMoveToTheEndBelow = new MenuItem( subMenu2, SWT.NONE );
    menuItemMoveToTheEndBelow.setText( "Below" );
    menuItemMoveToTheEndBelow.addSelectionListener( new SelectionAdapter() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        MWindow destWindow = part.getContext().get( MWindow.class );
        moveToTheEnd( partToBeMoved, destWindow, EModelService.BELOW );
      }
    } );
    MenuItem menuItemMoveToTheEndLeft = new MenuItem( subMenu2, SWT.NONE );
    menuItemMoveToTheEndLeft.setText( "to the Left" );
    menuItemMoveToTheEndLeft.addSelectionListener( new SelectionAdapter() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        MWindow destWindow = part.getContext().get( MWindow.class );
        moveToTheEnd( partToBeMoved, destWindow, EModelService.LEFT_OF );
      }
    } );
    MenuItem menuItemMoveToTheEndRight = new MenuItem( subMenu2, SWT.NONE );
    menuItemMoveToTheEndRight.setText( "to the Right" );
    menuItemMoveToTheEndRight.addSelectionListener( new SelectionAdapter() {

      /** The serialVersionUID of this DetachPartProvider.java */
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected( SelectionEvent e ) {
        MPart part = ( MPart )menu.getData( STACK_SELECTED_PART );
        MWindow destWindow = part.getContext().get( MWindow.class );
        moveToTheEnd( partToBeMoved, destWindow, EModelService.RIGHT_OF );
      }
    } );
  }

  /**
   * Moves a part into the same stack as another part is placed
   * 
   * @param partToBeMoved the reference to the part to be moved
   * @param destinationRefPart the reference to the other part, whose part stack will be the
   *          destination of the part to be moved
   */
  private void moveToPartStack( MPart partToBeMoved, MPart destinationRefPart ) {
    MWindow window = partToBeMoved.getContext().get( MWindow.class );
    MWindow destWindow = destinationRefPart.getContext().get( MWindow.class );
    if( partToBeMoved.equals( destinationRefPart )
        || ( !window.equals( destWindow ) && isPartSingleton( partToBeMoved ) ) )
    {
      // Do not allow singleton views to be moved to different windows
      return;
    }
    MPlaceholder ph1 = this.modelService.findPlaceholderFor( window, partToBeMoved );
    MPlaceholder ph2 = this.modelService.findPlaceholderFor( destWindow, destinationRefPart );
    if( ph1.getParent().equals( ph2.getParent() ) ) {
      return;
    }
    this.modelService.move( ph1, ph2.getParent() );
  }

  /**
   * Moves a part beside another part, sharing the Sash Container space
   * 
   * @param partToBeMoved the reference to the part to be moved
   * @param destinationRefPart the reference to the part used as reference for placing the moved
   *          part
   * @param position the relative position. One of {@link EModelService#ABOVE},
   *          {@link EModelService#BELOW}, {@link EModelService#LEFT_OF},
   *          {@link EModelService#RIGHT_OF}
   */
  private void moveBeside( MPart partToBeMoved, MPart destinationRefPart, int position ) {
    MWindow window = partToBeMoved.getContext().get( MWindow.class );
    MWindow destWindow = destinationRefPart.getContext().get( MWindow.class );
    if( ( !window.equals( destWindow ) && isPartSingleton( partToBeMoved ) ) ) {
      // Do not allow singleton views to be moved to different windows
      return;
    }
    MPlaceholder ph1 = this.modelService.findPlaceholderFor( window, partToBeMoved );
    MPlaceholder ph2 = this.modelService.findPlaceholderFor( destWindow, destinationRefPart );
    MPartSashContainerElement cont = ph2;
    cont = ( MPartSashContainerElement )cont.getParent();
    MPartStack ps = this.modelService.createModelElement( MPartStack.class );
    this.modelService.insert( ps, cont, position, 0.5f );
    ps.getChildren().add( ph1 );
    ps.setSelectedElement( ph1 );
  }

  /**
   * Moves a part to the border of the screen. It will be placed as an element of a new
   * {@link MPartSashContainer} and the other elemen will contain all the rest of the visible layout
   * 
   * @param partToBeMoved the reference to the part to be moved
   * @param position the relative position. One of {@link EModelService#ABOVE},
   *          {@link EModelService#BELOW}, {@link EModelService#LEFT_OF},
   *          {@link EModelService#RIGHT_OF}
   */
  private void moveToTheEnd( MPart partToBeMoved, MWindow destinationWindow, int position ) {
    MWindow window = partToBeMoved.getContext().get( MWindow.class );
    if( ( !window.equals( destinationWindow ) && isPartSingleton( partToBeMoved ) ) ) {
      // Do not allow singleton views to be moved to different windows
      return;
    }
    MPlaceholder ph1 = this.modelService.findPlaceholderFor( window, partToBeMoved );
    MPerspective persp = this.modelService.getActivePerspective( destinationWindow );
    MPartSashContainer psc = findDisplayedPartShashContainer( persp );
    MPartStack ps = this.modelService.createModelElement( MPartStack.class );
    this.modelService.insert( ps, psc, position, 0.3f );
    ps.getChildren().add( ph1 );
    ps.setSelectedElement( ph1 );
  }

  /**
   * Returns the first {@link MPartSashContainer} in the given perspective where all the elements
   * are visible.
   * 
   * @param persp the reference to the perspective
   * @return the first {@link MPartSashContainer} in the given perspective where all the elements
   *         are visible
   */
  private MPartSashContainer findDisplayedPartShashContainer( MPerspective persp ) {
    MElementContainer< ? > parentElem = ( MElementContainer< ? > )persp.getChildren().get( 0 );
    MPartSashContainer ret = null;
    while( ret == null && parentElem != null ) {
      boolean allToBeRendered = parentElem.getChildren().get( 0 ).isToBeRendered()
                                && parentElem.getChildren().get( 1 ).isToBeRendered();
      if( allToBeRendered && parentElem instanceof MPartSashContainer ) {
        ret = ( MPartSashContainer )parentElem;
      } else {
        MPartSashContainer renderedChild = null;
        for( MUIElement child : parentElem.getChildren() ) {
          if( child.isToBeRendered() && child instanceof MPartSashContainer ) {
            renderedChild = ( MPartSashContainer )child;
          }
        }
        parentElem = renderedChild;
      }
    }
    return ret;
  }

  /**
   * Returns <code>true</code> if the part is a ViewPart and its descriptor allows multiple
   * instances of the view; <code>false</code> otherwise
   * 
   * @param part the reference to the part
   * @return <code>true</code> if the part is a ViewPart and its descriptor allows multiple
   *         instances of the view; <code>false</code> otherwise
   */
  private boolean isPartSingleton( MPart part ) {
    boolean ret = false;
    IWorkbenchPart workbenchPpart = part.getContext().get( IWorkbenchPart.class );
    IViewDescriptor desctiptor = workbenchPpart.getSite()
      .getWorkbenchWindow()
      .getWorkbench()
      .getViewRegistry()
      .find( workbenchPpart.getSite().getId() );
    if( desctiptor != null ) {
      return !desctiptor.getAllowMultiple();
    }
    return ret;
  }
}
// -----------------------------------------------------------------------------
