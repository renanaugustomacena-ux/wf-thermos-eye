package com.alexcupsa.wifithermal.core.model

data class ChannelInfo(
    val channel: Int,
    val band: WifiBand,
    val frequency: Int,
    val apCount: Int,
    val overlappingApCount: Int = 0,
    val strongestSignal: Int,
    val averageSignal: Double,
    val congestionScore: Double,
)

data class ChannelAnalysisResult(
    val channels2g: List<ChannelInfo>,
    val channels5g: List<ChannelInfo>,
    val recommended2g: Int,
    val recommended5g: Int,
)
