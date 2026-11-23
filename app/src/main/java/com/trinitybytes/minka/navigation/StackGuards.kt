package com.trinitybytes.minka.navigation

/**
 * Pushes [key] unless it is already on top. Taps that navigate run outside a Nav3 entry's lifecycle,
 * so `dropUnlessResumed` cannot drop the second tap of a double tap; comparing with the top does.
 */
internal fun <T> pushOnce(stack: MutableList<T>, key: T) {
    if (stack.lastOrNull() != key) stack.add(key)
}

/** Pops the top only when it matches [isTop], so a repeated close never pops the screen below. */
internal fun <T> popIf(stack: MutableList<T>, isTop: (T) -> Boolean) {
    val top = stack.lastOrNull() ?: return
    if (stack.size > 1 && isTop(top)) stack.removeAt(stack.lastIndex)
}
