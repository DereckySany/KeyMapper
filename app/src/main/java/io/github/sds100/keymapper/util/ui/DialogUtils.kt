package io.github.sds100.keymapper.util.ui

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.databinding.DialogEdittextNumberBinding
import io.github.sds100.keymapper.databinding.DialogEdittextStringBinding
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.alertdialog.appcompat.*
import splitties.alertdialog.material.materialAlertDialog
import kotlin.coroutines.resume

/**
 * Created by sds100 on 30/03/2020.
 */

suspend fun Context.materialAlertDialog(
    lifecycleOwner: LifecycleOwner,
    model: PopupUi.Dialog
) = suspendCancellableCoroutine<DialogResponse?> { continuation ->

    materialAlertDialog {
        title = model.title
        setMessage(model.message)

        setPositiveButton(model.positiveButtonText) { _, _ ->
            continuation.resume(DialogResponse.POSITIVE)
        }

        setNeutralButton(model.neutralButtonText) { _, _ ->
            continuation.resume(DialogResponse.NEUTRAL)
        }

        setNegativeButton(model.negativeButtonText) { _, _ ->
            continuation.resume(DialogResponse.NEGATIVE)
        }

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)

            continuation.invokeOnCancellation {
                dismiss()
            }
        }
    }
}

suspend fun Context.materialAlertDialogCustomView(
    lifecycleOwner: LifecycleOwner,
    title: CharSequence,
    message: CharSequence,
    positiveButtonText: CharSequence? = null,
    neutralButtonText: CharSequence? = null,
    negativeButtonText: CharSequence? = null,
    view: View
) = suspendCancellableCoroutine<DialogResponse?> { continuation ->

    materialAlertDialog {
        setTitle(title)
        setMessage(message)

        setPositiveButton(positiveButtonText) { _, _ ->
            continuation.resume(DialogResponse.POSITIVE)
        }

        setNeutralButton(neutralButtonText) { _, _ ->
            continuation.resume(DialogResponse.NEUTRAL)
        }

        setNegativeButton(negativeButtonText) { _, _ ->
            continuation.resume(DialogResponse.NEGATIVE)
        }

        setView(view)

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)

            continuation.invokeOnCancellation {
                dismiss()
            }
        }
    }
}

suspend fun Context.multiChoiceDialog(
    lifecycleOwner: LifecycleOwner,
    items: List<MultiChoiceItem<*>>
) = suspendCancellableCoroutine<List<*>?> { continuation ->
    materialAlertDialog {
        val checkedItems = items
            .map { it.isChecked }
            .toBooleanArray()

        setMultiChoiceItems(
            items.map { it.label }.toTypedArray(),
            checkedItems
        ) { _, which, checked ->
            checkedItems[which] = checked
        }

        negativeButton(R.string.neg_cancel) { it.cancel() }

        okButton {
            val checkedItemIds = sequence {
                checkedItems.forEachIndexed { index, checked ->
                    if (checked) {
                        yield(items[index].id)
                    }
                }
            }.toList()

            continuation.resume(checkedItemIds)
        }

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)
        }
    }
}

suspend fun <ID> Context.singleChoiceDialog(
    lifecycleOwner: LifecycleOwner,
    items: List<Pair<ID, String>>
) = suspendCancellableCoroutine<ID?> { continuation ->
    materialAlertDialog {
        //message isn't supported
        setItems(
            items.map { it.second }.toTypedArray(),
        ) { _, position ->
            continuation.resume(items[position].first)
        }

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)
        }
    }
}

suspend fun Context.editTextStringAlertDialog(
    lifecycleOwner: LifecycleOwner,
    hint: String,
    allowEmpty: Boolean = false,
    initialText: String = "",
    inputType: Int? = null,
    message: CharSequence? = null,
) = suspendCancellableCoroutine<String?> { continuation ->

    val text = MutableStateFlow(initialText)

    val alertDialog = materialAlertDialog {
        val inflater = LayoutInflater.from(this@editTextStringAlertDialog)

        DialogEdittextStringBinding.inflate(inflater).apply {
            setHint(hint)
            setText(text)
            setAllowEmpty(allowEmpty)

            if (inputType != null) {
                editText.inputType = inputType
            }

            setView(this.root)
        }

        if (message != null) {
            this.message = message
        }

        okButton {
            continuation.resume(text.value)
        }

        negativeButton(R.string.neg_cancel) {
            it.cancel()
        }
    }

    //this prevents window leak
    alertDialog.resumeNullOnDismiss(continuation)
    alertDialog.dismissOnDestroy(lifecycleOwner)

    alertDialog.show()

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        text.collectLatest {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                if (allowEmpty) {
                    true
                } else {
                    it.isNotBlank()
                }
        }
    }
}

suspend fun Context.editTextNumberAlertDialog(
    lifecycleOwner: LifecycleOwner,
    hint: String,
    min: Int? = null,
    max: Int? = null,
) = suspendCancellableCoroutine<Int?> { continuation ->

    fun isValid(text: String?): Result<Int> {
        if (text.isNullOrBlank()) {
            return Error.InvalidNumber
        }

        return try {
            val num = text.toInt()

            min?.let {
                if (num < min) {
                    return Error.NumberTooSmall(min)
                }
            }

            max?.let {
                if (num > max) {
                    return Error.NumberTooBig(max)
                }
            }

            Success(num)
        } catch (e: NumberFormatException) {
            Error.InvalidNumber
        }
    }

    val resourceProvider = ServiceLocator.resourceProvider(this)
    val text = MutableStateFlow("")

    val inflater = LayoutInflater.from(this@editTextNumberAlertDialog)
    val binding = DialogEdittextNumberBinding.inflate(inflater).apply {
        setHint(hint)
        setText(text)
    }

    val alertDialog = materialAlertDialog {
        okButton {
            isValid(text.value).onSuccess { num ->
                continuation.resume(num)
            }
        }

        negativeButton(R.string.neg_cancel) { it.cancel() }

        setView(binding.root)
    }

    alertDialog.show()
    alertDialog.resumeNullOnDismiss(continuation)
    alertDialog.dismissOnDestroy(lifecycleOwner)

    lifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
        text.map { isValid(it) }
            .collectLatest { isValid ->
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    isValid.isSuccess

                binding.textInputLayout.error =
                    isValid.errorOrNull()?.getFullMessage(resourceProvider)
            }
    }
}

suspend fun Context.okDialog(
    lifecycleOwner: LifecycleOwner,
    message: String,
    title: String? = null,
) = suspendCancellableCoroutine<Unit?> { continuation ->

    val alertDialog = materialAlertDialog {

        setTitle(title)
        setMessage(message)

        okButton {
            continuation.resume(Unit)
        }
    }

    alertDialog.show()

    //this prevents window leak
    alertDialog.resumeNullOnDismiss(continuation)
    alertDialog.dismissOnDestroy(lifecycleOwner)
}

fun <T> Dialog.resumeNullOnDismiss(continuation: CancellableContinuation<T?>) {
    setOnDismissListener {
        if (!continuation.isCompleted) {
            continuation.resume(null)
        }
    }
}

fun Dialog.dismissOnDestroy(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            this@dismissOnDestroy.dismiss()
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}

object DialogUtils {
    fun keyMapperCrashedDialog(resourceProvider: ResourceProvider): PopupUi.Dialog {
        return PopupUi.Dialog(
            title = resourceProvider.getString(R.string.dialog_title_key_mapper_crashed),
            message = resourceProvider.getText(R.string.dialog_message_key_mapper_crashed),
            positiveButtonText = resourceProvider.getString(R.string.dialog_button_read_dont_kill_my_app_no),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
            neutralButtonText = resourceProvider.getString(R.string.dialog_button_read_dont_kill_my_app_yes)
        )
    }
}