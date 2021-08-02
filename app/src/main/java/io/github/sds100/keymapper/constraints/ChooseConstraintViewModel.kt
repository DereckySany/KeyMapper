package io.github.sds100.keymapper.constraints

import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 21/03/2020.
 */

class ChooseConstraintViewModel(
    private val useCase: CreateConstraintUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    companion object {
        private val ALL_CONSTRAINTS_ORDERED: Array<ChooseConstraintType> = arrayOf(
            ChooseConstraintType.APP_IN_FOREGROUND,
            ChooseConstraintType.APP_NOT_IN_FOREGROUND,
            ChooseConstraintType.APP_PLAYING_MEDIA,
            ChooseConstraintType.APP_NOT_PLAYING_MEDIA,
            ChooseConstraintType.MEDIA_PLAYING,
            ChooseConstraintType.MEDIA_NOT_PLAYING,

            ChooseConstraintType.BT_DEVICE_CONNECTED,
            ChooseConstraintType.BT_DEVICE_DISCONNECTED,

            ChooseConstraintType.SCREEN_ON,
            ChooseConstraintType.SCREEN_OFF,

            ChooseConstraintType.ORIENTATION_PORTRAIT,
            ChooseConstraintType.ORIENTATION_LANDSCAPE,
            ChooseConstraintType.ORIENTATION_0,
            ChooseConstraintType.ORIENTATION_90,
            ChooseConstraintType.ORIENTATION_180,
            ChooseConstraintType.ORIENTATION_270,

            ChooseConstraintType.FLASHLIGHT_ON,
            ChooseConstraintType.FLASHLIGHT_OFF,

            ChooseConstraintType.WIFI_ON,
            ChooseConstraintType.WIFI_OFF,
            ChooseConstraintType.WIFI_CONNECTED,
            ChooseConstraintType.WIFI_DISCONNECTED
        )
    }

    private val _listItems = MutableStateFlow<State<List<SimpleListItem>>>(State.Loading)
    val listItems = _listItems.asStateFlow()

    private val _returnResult = MutableSharedFlow<Constraint>()
    val returnResult = _returnResult.asSharedFlow()

    private var supportedConstraints = MutableStateFlow<Array<ChooseConstraintType>>(emptyArray())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            supportedConstraints.collectLatest {
                _listItems.value = State.Data(buildListItems())
            }
        }
    }

    fun setSupportedConstraints(supportedConstraints: Array<ChooseConstraintType>) {
        this.supportedConstraints.value = supportedConstraints
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            val constraintType = ChooseConstraintType.valueOf(id)

            when (constraintType) {
                ChooseConstraintType.APP_IN_FOREGROUND,
                ChooseConstraintType.APP_NOT_IN_FOREGROUND,
                ChooseConstraintType.APP_PLAYING_MEDIA,
                ChooseConstraintType.APP_NOT_PLAYING_MEDIA -> {
                    val packageName =
                        navigate("choose_package_for_constraint", NavDestination.ChooseApp)
                            ?: return@launch

                    val constraint = when (constraintType) {
                        ChooseConstraintType.APP_IN_FOREGROUND -> Constraint.AppInForeground(
                            packageName
                        )
                        ChooseConstraintType.APP_NOT_IN_FOREGROUND -> Constraint.AppNotInForeground(
                            packageName
                        )
                        ChooseConstraintType.APP_PLAYING_MEDIA -> Constraint.AppPlayingMedia(
                            packageName
                        )
                        ChooseConstraintType.APP_NOT_PLAYING_MEDIA -> Constraint.AppNotPlayingMedia(
                            packageName
                        )
                        else -> throw Exception("Don't know how to create $constraintType constraint after choosing app")
                    }

                    _returnResult.emit(constraint)
                }

                ChooseConstraintType.MEDIA_PLAYING -> _returnResult.emit(Constraint.MediaPlaying)
                ChooseConstraintType.MEDIA_NOT_PLAYING -> _returnResult.emit(Constraint.NoMediaPlaying)

                ChooseConstraintType.BT_DEVICE_CONNECTED,
                ChooseConstraintType.BT_DEVICE_DISCONNECTED -> {
                    val response = showPopup(
                        "bluetooth_device_constraint_limitation",
                        PopupUi.Ok(getString(R.string.dialog_message_bt_constraint_limitation))
                    )

                    response ?: return@launch
                    val device = navigate(
                        "choose_bluetooth_device_for_constraint",
                        NavDestination.ChooseBluetoothDevice
                    )
                        ?: return@launch

                    val constraint = when (constraintType) {
                        ChooseConstraintType.BT_DEVICE_CONNECTED -> Constraint.BtDeviceConnected(
                            device.address,
                            device.name
                        )
                        ChooseConstraintType.BT_DEVICE_DISCONNECTED -> Constraint.BtDeviceDisconnected(
                            device.address,
                            device.name
                        )
                        else -> throw Exception("Don't know how to create $constraintType constraint after choosing app")
                    }

                    _returnResult.emit(constraint)
                }

                ChooseConstraintType.SCREEN_ON -> {
                    val response = showPopup(
                        "screen_on_constraint_limitation",
                        PopupUi.Ok(getString(R.string.dialog_message_screen_constraints_limitation))
                    )

                    response ?: return@launch

                    _returnResult.emit(Constraint.ScreenOn)
                }

                ChooseConstraintType.SCREEN_OFF -> {
                    val response = showPopup(
                        "screen_off_constraint_limitation",
                        PopupUi.Ok(getString(R.string.dialog_message_screen_constraints_limitation))
                    )

                    response ?: return@launch

                    _returnResult.emit(Constraint.ScreenOff)
                }

                ChooseConstraintType.ORIENTATION_PORTRAIT ->
                    _returnResult.emit(Constraint.OrientationPortrait)

                ChooseConstraintType.ORIENTATION_LANDSCAPE ->
                    _returnResult.emit(Constraint.OrientationLandscape)

                ChooseConstraintType.ORIENTATION_0 ->
                    _returnResult.emit(Constraint.OrientationCustom(Orientation.ORIENTATION_0))

                ChooseConstraintType.ORIENTATION_90 ->
                    _returnResult.emit(Constraint.OrientationCustom(Orientation.ORIENTATION_90))

                ChooseConstraintType.ORIENTATION_180 ->
                    _returnResult.emit(Constraint.OrientationCustom(Orientation.ORIENTATION_180))

                ChooseConstraintType.ORIENTATION_270 ->
                    _returnResult.emit(Constraint.OrientationCustom(Orientation.ORIENTATION_270))

                ChooseConstraintType.FLASHLIGHT_ON -> {
                    val lens = chooseFlashlightLens() ?: return@launch
                    _returnResult.emit(Constraint.FlashlightOn(lens))
                }

                ChooseConstraintType.FLASHLIGHT_OFF -> {
                    val lens = chooseFlashlightLens() ?: return@launch
                    _returnResult.emit(Constraint.FlashlightOff(lens))
                }

                ChooseConstraintType.WIFI_ON -> {
                    _returnResult.emit(Constraint.WifiOn)
                }

                ChooseConstraintType.WIFI_OFF -> {
                    _returnResult.emit(Constraint.WifiOff)
                }

                ChooseConstraintType.WIFI_CONNECTED, ChooseConstraintType.WIFI_DISCONNECTED -> {
                    val knownSSIDs = useCase.getKnownWiFiSSIDs()

                    val chosenSSID: String?

                    if (knownSSIDs == null) {
                        val dialog = PopupUi.Text(
                            hint = getString(R.string.hint_wifi_ssid),
                            allowEmpty = true,
                            message = getString(R.string.constraint_wifi_message_cant_list_networks)
                        )

                        val ssidText = showPopup("type_ssid", dialog) ?: return@launch

                        if (ssidText.isBlank()) {
                            chosenSSID = null
                        } else {
                            chosenSSID = ssidText
                        }
                    } else {
                        val anySSIDItem =
                            "any" to getString(R.string.constraint_wifi_pick_network_any)

                        val ssidItems = knownSSIDs.map { "ssid_$it" to it }

                        val items = listOf(anySSIDItem).plus(ssidItems)

                        val chosenItem =
                            showPopup("choose_ssid", PopupUi.SingleChoice(items)) ?: return@launch

                        if (chosenItem == anySSIDItem.first) {
                            chosenSSID = null
                        } else {
                            chosenSSID = items.single { it.first == chosenItem }.second
                        }
                    }

                    when (constraintType) {
                        ChooseConstraintType.WIFI_CONNECTED ->
                            _returnResult.emit(Constraint.WifiConnected(chosenSSID))

                        ChooseConstraintType.WIFI_DISCONNECTED ->
                            _returnResult.emit(Constraint.WifiDisconnected(chosenSSID))
                    }
                }
            }
        }
    }

    private suspend fun chooseFlashlightLens(): CameraLens? {
        val items = listOf(
            CameraLens.FRONT to getString(R.string.lens_front),
            CameraLens.BACK to getString(R.string.lens_back)
        )

        val dialog = PopupUi.SingleChoice(items)

        val cameraLens = showPopup("choose_flashlight_lens", dialog)

        return cameraLens
    }

    private fun buildListItems(): List<DefaultSimpleListItem> = sequence {
        ALL_CONSTRAINTS_ORDERED.forEach { type ->
            if (!supportedConstraints.value.contains(type)) return@forEach

            val title: String = when (type) {
                ChooseConstraintType.APP_IN_FOREGROUND -> getString(R.string.constraint_choose_app_foreground)
                ChooseConstraintType.APP_NOT_IN_FOREGROUND -> getString(R.string.constraint_choose_app_not_foreground)
                ChooseConstraintType.APP_PLAYING_MEDIA -> getString(R.string.constraint_choose_app_playing_media)
                ChooseConstraintType.APP_NOT_PLAYING_MEDIA -> getString(R.string.constraint_choose_app_not_playing_media)
                ChooseConstraintType.MEDIA_NOT_PLAYING -> getString(R.string.constraint_choose_media_not_playing)
                ChooseConstraintType.MEDIA_PLAYING -> getString(R.string.constraint_choose_media_playing)
                ChooseConstraintType.BT_DEVICE_CONNECTED -> getString(R.string.constraint_choose_bluetooth_device_connected)
                ChooseConstraintType.BT_DEVICE_DISCONNECTED -> getString(R.string.constraint_choose_bluetooth_device_disconnected)
                ChooseConstraintType.SCREEN_ON -> getString(R.string.constraint_choose_screen_on_description)
                ChooseConstraintType.SCREEN_OFF -> getString(R.string.constraint_choose_screen_off_description)
                ChooseConstraintType.ORIENTATION_PORTRAIT -> getString(R.string.constraint_choose_orientation_portrait)
                ChooseConstraintType.ORIENTATION_LANDSCAPE -> getString(R.string.constraint_choose_orientation_landscape)
                ChooseConstraintType.ORIENTATION_0 -> getString(R.string.constraint_choose_orientation_0)
                ChooseConstraintType.ORIENTATION_90 -> getString(R.string.constraint_choose_orientation_90)
                ChooseConstraintType.ORIENTATION_180 -> getString(R.string.constraint_choose_orientation_180)
                ChooseConstraintType.ORIENTATION_270 -> getString(R.string.constraint_choose_orientation_270)
                ChooseConstraintType.FLASHLIGHT_ON -> getString(R.string.constraint_flashlight_on)
                ChooseConstraintType.FLASHLIGHT_OFF -> getString(R.string.constraint_flashlight_off)
                ChooseConstraintType.WIFI_ON -> getString(R.string.constraint_wifi_on)
                ChooseConstraintType.WIFI_OFF -> getString(R.string.constraint_wifi_off)
                ChooseConstraintType.WIFI_CONNECTED -> getString(R.string.constraint_wifi_connected)
                ChooseConstraintType.WIFI_DISCONNECTED -> getString(R.string.constraint_wifi_disconnected)
            }

            val error = useCase.isSupported(type)

            val listItem = DefaultSimpleListItem(
                id = type.toString(),
                title = title,
                isEnabled = error == null,
                subtitle = error?.getFullMessage(this@ChooseConstraintViewModel),
                subtitleTint = TintType.Error,
                icon = null
            )

            yield(listItem)
        }
    }.toList()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val isSupported: CreateConstraintUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseConstraintViewModel(isSupported, resourceProvider) as T
        }
    }
}