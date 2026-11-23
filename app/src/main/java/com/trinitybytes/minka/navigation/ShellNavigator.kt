package com.trinitybytes.minka.navigation

import androidx.navigation3.runtime.NavKey

/**
 * What a feature screen inside the signed-in shell can ask the shell to do. Feature entry functions
 * receive it, so later features add destinations without editing [MinkaShell].
 */
interface ShellNavigator {
    /** Pushes [key] onto the back stack of the selected tab (detail, owner profile). */
    fun open(key: NavKey)

    /** Pops the selected tab's back stack; on a tab root it returns to Inicio. */
    fun back()

    /** Switches to Buscar and asks its field to take focus (Figma 2.02, audit finding 16). */
    fun openSearch()

    /** Opens Publicar full screen, resuming [draftId] when given (Perfil › Mis publicaciones). */
    fun publish(draftId: String? = null)
}
