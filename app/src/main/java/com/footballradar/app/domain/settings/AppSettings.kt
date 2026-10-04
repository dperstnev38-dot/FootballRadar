package com.footballradar.app.domain.settings

data class AppSettings(
    val colors: Map<ThemeColorSetting, String> = ThemeColorSetting.entries.associate {
        it to it.defaultHex
    },
) {
    fun color(setting: ThemeColorSetting): String =
        colors[setting] ?: setting.defaultHex
}
