package com.footballradar.app.domain.settings

enum class ThemeColorSetting(
    val title: String,
    val defaultHex: String,
) {
    ACCENT("Основной цвет / акцент", "#38D39F"),
    BACKGROUND("Фон приложения", "#101512"),
    MATCH_CARD("Карточки матчей", "#1A211E"),
    LIVE("Цвет LIVE", "#FF5263"),
    RADAR_EVENT("События радара", "#76B7FF"),
}
