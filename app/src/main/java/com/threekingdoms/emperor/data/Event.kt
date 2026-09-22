package com.threekingdoms.emperor.data

/**
 * 随机事件。
 */
data class EventChoice(
    val label: String,
    val goldEffect: Int = 0,
    val moraleEffect: Int = 0,
    val staminaEffect: Int = 0,
    val loyaltyEffect: Int = 0,
    val authorityEffect: Int = 0,
    val relationshipEffect: Int = 0
)

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val choices: List<EventChoice>
) {
    companion object {
        // 事件池
        val POOL = listOf(
            Event("e1", "南方有祥瑞",
                "南方有凤凰现世，官员请陛下遣使祭祀。",
                listOf(
                    EventChoice("遣使祭祀", goldEffect = -500, authorityEffect = 5, moraleEffect = 3),
                    EventChoice("无视之", moraleEffect = -3)
                )
            ),
            Event("e2", "刺客潜入",
                "宫中侍卫捉到一名刺客，审讯后供出背后主使。",
                listOf(
                    EventChoice("严刑拷打后处死", authorityEffect = 3, moraleEffect = -2),
                    EventChoice("赦免以彰显仁德", benevolence = 5),
                    EventChoice("交由刑部审讯", goldEffect = 200)
                )
            ).let { e ->
                e.copy(choices = listOf(
                    EventChoice("严刑拷打", authorityEffect = 3, moraleEffect = -2),
                    EventChoice("赦免以彰显仁德", authorityEffect = -2, moraleEffect = 5),
                    EventChoice("交由刑部审讯", goldEffect = 200)
                ))
            },
            Event("e3", "黄河泛滥",
                "黄河决堤，三州受灾。",
                listOf(
                    EventChoice("拨银赈灾", goldEffect = -2000, moraleEffect = 8),
                    EventChoice("命地方自救", moraleEffect = -5),
                    EventChoice("减税三年", goldEffect = -1000, moraleEffect = 6, authorityEffect = -2)
                )
            ),
            Event("e4", "异士来访",
                "有一奇人求见，自称能观星象、知未来。",
                listOf(
                    EventChoice("延请入宫", authorityEffect = 3, goldEffect = -300),
                    EventChoice("赐金放还", moraleEffect = 1)
                )
            ),
            Event("e5", "外族进贡",
                "北方匈奴遣使进贡良马百匹。",
                listOf(
                    EventChoice("纳贡赐宴", goldEffect = -500, relationshipEffect = 10),
                    EventChoice("拒绝并备战", authorityEffect = 5, relationshipEffect = -20)
                )
            ),
            Event("e6", "妃嫔争宠",
                "后宫两位妃嫔起了争执，闹得宫内不宁。",
                listOf(
                    EventChoice("各罚月俸", moraleEffect = -2),
                    EventChoice("调和封赏", goldEffect = -500, moraleEffect = 2)
                )
            ),
            Event("e7", "军中疫病",
                "军营爆发疫病，士兵折损不少。",
                listOf(
                    EventChoice("急调太医", goldEffect = -800, moraleEffect = 3),
                    EventChoice("隔离军营", moraleEffect = -5)
                )
            ),
            Event("e8", "谶纬传言",
                "民间流传'天命在君'的谶纬。",
                listOf(
                    EventChoice("默认不表态", authorityEffect = 2),
                    EventChoice("严禁传播", moraleEffect = -3, authorityEffect = 3)
                )
            ),
            Event("e9", "名将病重",
                "一名忠臣卧病不起，群臣请陛下探视。",
                listOf(
                    EventChoice("亲往探视", loyaltyEffect = 8, staminaEffect = -5),
                    EventChoice("遣御医问诊", goldEffect = -300, loyaltyEffect = 4)
                )
            ),
            Event("e10", "边境冲突",
                "边疆有外族袭扰，戍将请增援。",
                listOf(
                    EventChoice("调兵增援", goldEffect = -1000, moraleEffect = 2),
                    EventChoice("固守不出", moraleEffect = -5, authorityEffect = -3)
                )
            ),
            Event("e11", "商旅不通",
                "商路被匪贼阻断，市面物价飞涨。",
                listOf(
                    EventChoice("派兵剿匪", goldEffect = -1500, moraleEffect = 4),
                    EventChoice("招商自筹", goldEffect = 800, moraleEffect = -3)
                )
            ),
            Event("e12", "群臣宴饮",
                "有大臣请陛下参加宴饮。",
                listOf(
                    EventChoice("欣然赴宴", staminaEffect = 10, loyaltyEffect = 3),
                    EventChoice("婉拒以示勤政", authorityEffect = 3)
                )
            )
        )
    }
}