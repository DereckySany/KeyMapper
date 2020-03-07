package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 25/01/2020.
 */

data class KeymapListItemModel(
    val id: Long,
    val actionList: List<ActionModel>,
    val triggerModel: TriggerModel,
    val flagList: List<FlagModel>,
    val isEnabled: Boolean
)