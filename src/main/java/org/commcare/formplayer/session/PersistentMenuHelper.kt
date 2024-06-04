package org.commcare.formplayer.session

import org.commcare.cases.util.StringUtils
import org.commcare.formplayer.beans.menus.PeristentCommand
import org.commcare.util.screen.EntityScreen
import org.commcare.util.screen.MenuScreen
import org.commcare.util.screen.MultiSelectEntityScreen
import org.commcare.util.screen.Screen

/**
 * Utility methods related to persistent menu evaluation
 */
class PersistentMenuHelper {

    var persistentMenu: ArrayList<PeristentCommand> = ArrayList()
    private var currentMenu: PeristentCommand? = null

    fun addEntitySelection(input: String, entityText: String) {
        if (!StringUtils.isEmpty(input) && !input.contentEquals(MultiSelectEntityScreen.USE_SELECTED_VALUES)) {
            val command = PeristentCommand(input, entityText)
            addPersistentCommand(command)
        }
    }

    fun addMenusToPeristentMenu(menuScreen: MenuScreen) {
        val options = menuScreen.getOptions()
        for (i in options.indices) {
            val command = PeristentCommand(i.toString(), options[i])
            addPersistentCommand(command)
        }
    }

    /**
     * Identifies and sets current menu based on the input give to the current screen
     */
     fun advanceCurrentMenuWithInput(screen: Screen, input: String) {
        if (screen is MenuScreen) {
            val index = input.toInt()
            if (currentMenu != null && index < currentMenu!!.commands.size) {
                currentMenu =  currentMenu!!.commands[index]
            } else if (index < persistentMenu!!.size) {
                currentMenu =  persistentMenu!![index]
            }
        } else if (screen is EntityScreen) {
            check(currentMenu != null) { "Current menu can't be null for Entity screen" }
            check(currentMenu!!.commands.size <= 1) { "Current menu can't have more than one commands for Entity screen" }

            // if it's the last input, we would not have yet added entity screen menu to peristent Menu, so just
            // return current menu otherwise return the only command entry for entity screen
            currentMenu =  if (currentMenu!!.commands.size == 0) currentMenu else currentMenu!!.commands[0]
        }
    }

    private fun addPersistentCommand(command: PeristentCommand) {
        if (currentMenu == null) {
            persistentMenu.add(command)
        } else {
            currentMenu!!.addCommand(command)
        }
    }
}
