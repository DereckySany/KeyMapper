package io.github.sds100.keymapper.system.accessibility

import io.github.sds100.keymapper.util.InputEventType
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 17/04/2021.
 */
interface IAccessibilityService {
    fun doGlobalAction(action: Int): Result<*>

    fun tapScreen(x: Int, y: Int, inputEventType: InputEventType): Result<*>

    val isFingerprintGestureDetectionAvailable: Boolean

    var serviceFlags: Int?
    var serviceFeedbackType: Int?
    var serviceEventTypes: Int?
    
    fun performActionOnNode(
        findNode: (node: AccessibilityNodeModel) -> Boolean,
        performAction: (node: AccessibilityNodeModel) -> AccessibilityNodeAction?
    ): Result<*>

    val rootNode: AccessibilityNodeModel?

    fun hideKeyboard()
    fun showKeyboard()
    val isKeyboardHidden: Flow<Boolean>

    fun switchIme(imeId: String)

    fun disableSelf()

    fun findFocussedNode(focus: Int): AccessibilityNodeModel?
}