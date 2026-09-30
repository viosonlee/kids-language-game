package com.kidslanguage.companion.feature.repeat

enum class RepeatProblem(val message: String) {
    PERMISSION("麦克风还没打开，可以先听一听、玩选图。"),
    NO_SPEECH("还没听清，点麦克风再说一次吧。"),
    RECORDING("暂时听不到你，可以再试一次，或先玩选图。"),
    MODEL("说话练习还没准备好，可以先听一听、玩选图。"),
    CONTENT("这里暂时没有可玩的题目，换个场景吧。"),
}
