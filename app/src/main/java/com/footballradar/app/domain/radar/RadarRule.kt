package com.footballradar.app.domain.radar

import com.footballradar.app.domain.model.FootballMatch

interface RadarRule {
    fun evaluate(previous: FootballMatch?, current: FootballMatch): List<RadarEvent>
}
