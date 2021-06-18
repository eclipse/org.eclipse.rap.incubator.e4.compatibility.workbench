/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 472654
 ******************************************************************************/

package org.eclipse.ui.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.e4.core.commands.internal.HandlerServiceImpl;
import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.internal.workbench.addons.CommandProcessingAddon;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCategory;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.internal.commands.CommandPersistence;

/**
 * @since 3.5
 *
 */
public class CommandToModelProcessor {

 // RAP [DM]:  
    
//  private Map<String, MCategory> categories = new HashMap<>();
//
//  private Map<String, MCommand> commands = new HashMap<>();
    
 // RAPEND: [DM]

	private EModelService modelService;

	@Execute
	void process(MApplication application, final IEclipseContext context, EModelService modelService) {
	    
	    // RAP [DM]:  
	    Map<String, MCategory> categories = new HashMap<>();

	    Map<String, MCommand> commands = new HashMap<>();
	    // RAPEND: [DM]
	    
		this.modelService = modelService;
		for (MCategory catModel : application.getCategories()) {
			categories.put(catModel.getElementId(), catModel);
		}

		for (MCommand cmdModel : application.getCommands()) {
			commands.put(cmdModel.getElementId(), cmdModel);
		}
		CommandManager commandManager = context.get(CommandManager.class);
		if (commandManager == null) {
			HandlerServiceImpl.handlerGenerator = new ContextFunction() {
			    @Override
                public Object compute(IEclipseContext paramContext, String contextKey) {
                    IEclipseContext cont = paramContext;
                    if (paramContext == null)
                    {
                        cont = context;
                    }
                    return new WorkbenchHandlerServiceHandler(contextKey, cont);
                }
			};
			commandManager = new CommandManager();
			// setCommandFireEvents(commandManager, false);
			context.set(CommandManager.class, commandManager);
		}

		CommandPersistence cp = new CommandPersistence(commandManager);
		ContextInjectionFactory.inject(cp, context);
		cp.reRead();
		
		 // RAP [DM]:  
//	      generateCategories(application, commandManager);
//        generateCommands(application, commandManager);
		generateCategories(application, commandManager, categories);
		generateCommands(application, commandManager, commands, categories);
		cp.dispose();
	}

	/**
	 * @param application
	 * @param commandManager
	 * @param commands 
	 * @param categories 
	 */
	private void generateCommands(MApplication application, CommandManager commandManager, Map<String, MCommand> commands, Map<String, MCategory> categories) {
		for (Command cmd : commandManager.getDefinedCommands()) {
			final MCommand mCommand = commands.get(cmd.getId());
			if (mCommand != null) {
				try {
					// This is needed to set the command name and description using the correct locale.
					String cmdName = cmd.getName();
					if (!cmdName.equals(mCommand.getCommandName())) {
						mCommand.setCommandName(cmdName);
						String cmdDesc = cmd.getDescription();
						if (cmdDesc != null)
							mCommand.setDescription(cmdDesc);
					}
				} catch (NotDefinedException e) {
					// Since we asked for defined commands, this shouldn't be an issue
					WorkbenchPlugin.log(e);
				}
				continue;
			}
			try {
				final MCategory categoryModel = categories.get(cmd.getCategory().getId());

				MCommand command = CommandProcessingAddon.createCommand(cmd, modelService, categoryModel);

				application.getCommands().add(command);
				commands.put(command.getElementId(), command);
			} catch (NotDefinedException e) {
				WorkbenchPlugin.log(e);
			}
		}
	}



	/**
	 * @param commandManager
	 * @param categories 
	 */
	private void generateCategories(MApplication application, CommandManager commandManager, Map<String, MCategory> categories) {
		for (Category cat : commandManager.getDefinedCategories()) {
			if (categories.containsKey(cat.getId())) {
				continue;
			}
			try {
				MCategory catModel = modelService.createModelElement(MCategory.class);
				catModel.setElementId(cat.getId());
				catModel.setName(cat.getName());
				catModel.setDescription(cat.getDescription());
				application.getCategories().add(catModel);
				categories.put(catModel.getElementId(), catModel);
			} catch (NotDefinedException e) {
				// Since we asked for defined commands, this shouldn't be an
				// issue
				WorkbenchPlugin.log(e);
			}
		}
	}

}