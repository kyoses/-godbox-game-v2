package com.threekingdoms.emperor.data

/**
 * 6 个剧本种子数据 + 加载入口。
 */
object ScenarioLoader {

    private val SCENARIOS = listOf(
        caocao(), liubei(), sunquan(), dongzhuo(), hejin(), baiqingchenqingzhi()
    )

    fun all(): List<Scenario> = SCENARIOS

    fun load(id: String): GameState {
        val s = SCENARIOS.find { it.id == id } ?: SCENARIOS.first()
        GameStateRepository.scenarioId = id
        GameStateRepository.phase = GamePhase.MAIN

        // 初始化势力关系（玩家对其他势力起始关系）
        val forces = mutableListOf(
            Force(Force.HAN, "汉室", Force.colorOf(Force.HAN), treasury = 5000),
            Force(Force.WEI, "魏", Force.colorOf(Force.WEI), treasury = 8000),
            Force(Force.SHU, "蜀", Force.colorOf(Force.SHU), treasury = 7000),
            Force(Force.WU, "吴", Force.colorOf(Force.WU), treasury = 7000),
            Force(Force.QUNXIONG, "群雄", Force.colorOf(Force.QUNXIONG), treasury = 6000),
            Force(Force.XIONGNU, "匈奴", Force.colorOf(Force.XIONGNU), treasury = 4000)
        )
        // 玩家对其他势力起始关系
        val playerForceId = s.initialEmperor.force
        forces.forEach { f ->
            f.setRelationship(playerForceId, when (f.id) {
                playerForceId -> 100
                else -> 0
            })
        }

        val state = GameState(
            emperor = s.initialEmperor.copy(),
            ministers = s.initialMinisters.map { it.copy() }.toMutableList(),
            memorials = s.initialMemorials.map { it.copy() }.toMutableList(),
            prefectures = s.initialPrefectures.map { it.copy() }.toMutableList(),
            consorts = s.initialConsorts.map { it.copy() }.toMutableList(),
            forces = forces,
            gold = 12000,
            peopleMorale = 75,
            troopTotal = 80000
        )
        GameStateRepository.state = state
        return state
    }

    // ===================== 曹操剧本 =====================

    private fun caocao(): Scenario {
        val emperor = Emperor(
            name = "曹操", dynasty = "魏", force = "魏",
            capitalPrefecture = "许昌",
            year = 1, month = 1, day = 1, shichen = 0,
            power = 78, intellect = 92, leadership = 95,
            benevolence = 60, authority = 88, charisma = 75
        )
        val ministers = listOf(
            m("xiahoudun", "夏侯惇", "大将军", "魏", 88, 65, 85, 70, 95, 90),
            m("xiahouyuan", "夏侯渊", "骠骑将军", "魏", 90, 60, 80, 65, 90, 85),
            m("caoren", "曹仁", "征南将军", "魏", 82, 70, 85, 70, 88, 85),
            m("caohong", "曹洪", "虎威将军", "魏", 80, 55, 75, 70, 85, 80),
            m("chengyu", "程昱", "谋士", "魏", 35, 95, 75, 75, 92, 90),
            m("xunyu", "荀彧", "尚书令", "魏", 40, 96, 88, 88, 95, 95),
            m("guojia", "郭嘉", "军祭酒", "魏", 30, 98, 70, 65, 90, 92),
            m("zhangliao", "张辽", "征东将军", "魏", 92, 75, 88, 75, 88, 85)
        )
        val consorts = listOf(
            c("bian", "卞氏", 85, 70, "曹氏"),
            c("liu", "刘氏", 80, 65, "汉室宗亲")
        )
        val memorials = listOf(
            memorialTax(), memorialMilitary("张辽"), memorialPersonnel("荀彧")
        )
        val prefs = defaultPrefectures().toMutableList().apply {
            this.find { it.id == "xuchang" }?.let {
                it.governor = "xiahoudun"
                it.ownerForce = "魏"
            }
            this.find { it.id == "ye" }?.let {
                it.governor = "xunyu"
                it.ownerForce = "魏"
            }
        }
        return Scenario(
            id = "caocao", name = "曹操", dynasty = "魏",
            era = "建安元年", difficulty = "中等",
            capital = "许昌", force = "魏",
            description = "挟天子以令诸侯，雄踞中原",
            initialEmperor = emperor,
            initialMinisters = ministers,
            initialConsorts = consorts,
            initialMemorials = memorials,
            initialPrefectures = prefs
        )
    }

    // ===================== 刘备剧本 =====================

    private fun liubei(): Scenario {
        val emperor = Emperor(
            name = "刘备", dynasty = "蜀", force = "蜀",
            capitalPrefecture = "成都",
            year = 1, month = 1, day = 1, shichen = 0,
            power = 78, intellect = 78, leadership = 88,
            benevolence = 95, authority = 85, charisma = 92
        )
        val ministers = listOf(
            m("guanyu", "关羽", "前将军", "蜀", 98, 75, 92, 85, 95, 95),
            m("zhangfei", "张飞", "右将军", "蜀", 96, 50, 80, 70, 88, 92),
            m("zhaoyun", "赵云", "翊军将军", "蜀", 95, 70, 88, 80, 92, 95),
            m("machao", "马超", "骠骑将军", "蜀", 94, 60, 82, 65, 80, 80),
            m("huangzhong", "黄忠", "后将军", "蜀", 90, 60, 80, 75, 85, 85),
            m("zhugeliang", "诸葛亮", "丞相", "蜀", 40, 100, 95, 95, 100, 100),
            m("pangtong", "庞统", "军师中郎将", "蜀", 35, 95, 80, 80, 90, 85),
            m("fawei", "法正", "尚书令", "蜀", 30, 92, 85, 75, 88, 85)
        )
        val consorts = listOf(
            c("gan", "甘夫人", 80, 65, "平民"),
            c("mi", "糜夫人", 85, 60, "糜氏")
        )
        val memorials = listOf(
            memorialPersonnel("诸葛亮"),
            memorialDiplomacy("法正"),
            memorialPublicWorks("黄忠")
        )
        val prefs = defaultPrefectures().toMutableList().apply {
            this.find { it.id == "chengdu" }?.let {
                it.governor = "zhugeliang"
                it.ownerForce = "蜀"
            }
        }
        return Scenario(
            id = "liubei", name = "刘备", dynasty = "蜀",
            era = "章武元年", difficulty = "困难",
            capital = "成都", force = "蜀",
            description = "匡扶汉室，三顾茅庐",
            initialEmperor = emperor,
            initialMinisters = ministers,
            initialConsorts = consorts,
            initialMemorials = memorials,
            initialPrefectures = prefs
        )
    }

    // ===================== 孙权剧本 =====================

    private fun sunquan(): Scenario {
        val emperor = Emperor(
            name = "孙权", dynasty = "吴", force = "吴",
            capitalPrefecture = "建业",
            year = 1, month = 1, day = 1, shichen = 0,
            power = 78, intellect = 88, leadership = 90,
            benevolence = 75, authority = 82, charisma = 88
        )
        val ministers = listOf(
            m("zhouyu", "周瑜", "大都督", "吴", 85, 92, 92, 80, 95, 95),
            m("luxun", "陆逊", "丞相", "吴", 70, 96, 92, 85, 92, 90),
            m("lvmeng", "吕蒙", "虎威将军", "吴", 85, 80, 88, 75, 88, 88),
            m("ganning", "甘宁", "折冲将军", "吴", 90, 60, 80, 70, 85, 85),
            m("taishici", "太史慈", "建昌都尉", "吴", 88, 65, 80, 75, 88, 85),
            m("luxing", "鲁肃", "赞军校尉", "吴", 50, 88, 88, 90, 90, 92),
            m("zhugejin", "诸葛瑾", "绥南将军", "吴", 40, 88, 80, 90, 90, 88)
        )
        val consorts = listOf(
            c("sunshangxiang", "孙尚香", 95, 80, "吴宗室"),
            c("xu", "徐氏", 78, 75, "平民")
        )
        val memorials = listOf(
            memorialMilitary("周瑜"),
            memorialTax(),
            memorialPublicWorks("陆逊")
        )
        val prefs = defaultPrefectures().toMutableList().apply {
            this.find { it.id == "jianye" }?.let {
                it.governor = "luxun"
                it.ownerForce = "吴"
            }
        }
        return Scenario(
            id = "sunquan", name = "孙权", dynasty = "吴",
            era = "黄武元年", difficulty = "中等",
            capital = "建业", force = "吴",
            description = "坐断东南战未休",
            initialEmperor = emperor,
            initialMinisters = ministers,
            initialConsorts = consorts,
            initialMemorials = memorials,
            initialPrefectures = prefs
        )
    }

    // ===================== 董卓剧本 =====================

    private fun dongzhuo(): Scenario {
        val emperor = Emperor(
            name = "董卓", dynasty = "汉", force = "群雄",
            capitalPrefecture = "长安",
            year = 1, month = 1, day = 1, shichen = 0,
            power = 92, intellect = 55, leadership = 80,
            benevolence = 20, authority = 95, charisma = 40
        )
        val ministers = listOf(
            m("liru", "李儒", "太师", "群雄", 60, 90, 70, 50, 80, 80),
            m("lvbu", "吕布", "中郎将", "群雄", 100, 50, 85, 30, 50, 70),
            m("huaxiong", "华雄", "都督", "群雄", 90, 50, 75, 30, 60, 65),
            m("lijue", "李傕", "中郎将", "群雄", 80, 60, 70, 30, 60, 60),
            m("guosi", "郭汜", "中郎将", "群雄", 78, 55, 68, 30, 60, 60),
            m("jiaxu", "贾诩", "谋士", "群雄", 30, 98, 75, 60, 70, 75)
        )
        val consorts = listOf(
            c("diaochan", "貂蝉", 100, 85, "王允义女")
        )
        val memorials = listOf(
            memorialTax(),
            memorialJustice(),
            memorialDiplomacy("李儒")
        )
        val prefs = defaultPrefectures().toMutableList().apply {
            this.find { it.id == "chang'an" }?.let {
                it.governor = "liru"
                it.ownerForce = "群雄"
            }
        }
        return Scenario(
            id = "dongzhuo", name = "董卓", dynasty = "汉",
            era = "初平元年", difficulty = "极难",
            capital = "长安", force = "群雄",
            description = "暴虐天下，群雄并起",
            initialEmperor = emperor,
            initialMinisters = ministers,
            initialConsorts = consorts,
            initialMemorials = memorials,
            initialPrefectures = prefs
        )
    }

    // ===================== 何进剧本 =====================

    private fun hejin(): Scenario {
        val emperor = Emperor(
            name = "何进", dynasty = "汉", force = "汉",
            capitalPrefecture = "洛阳",
            year = 1, month = 1, day = 1, shichen = 0,
            power = 65, intellect = 55, leadership = 60,
            benevolence = 50, authority = 75, charisma = 50
        )
        val ministers = listOf(
            m("hejin", "何进", "大将军", "汉", 70, 55, 70, 40, 70, 80),
            m("hemiao", "何苗", "车骑将军", "汉", 65, 40, 55, 35, 60, 70),
            m("jianshuo", "蹇硕", "中常侍", "汉", 60, 50, 60, 30, 50, 50),
            m("zhangrang", "张让", "中常侍", "汉", 30, 70, 40, 30, 30, 30),
            m("hankui", "韩馥", "豫州刺史", "汉", 55, 60, 70, 65, 80, 80)
        )
        val consorts = listOf(
            c("zhang", "张氏", 75, 60, "何氏外戚"),
            c("zhao", "赵氏", 70, 55, "平民")
        )
        val memorials = listOf(
            memorialTax(),
            memorialMilitary("何进"),
            memorialPersonnel("蹇硕")
        )
        val prefs = defaultPrefectures().toMutableList().apply {
            this.find { it.id == "luoyang" }?.let {
                it.governor = "hejin"
                it.ownerForce = "汉"
            }
        }
        return Scenario(
            id = "hejin", name = "何进", dynasty = "汉",
            era = "光熹元年", difficulty = "极难",
            capital = "洛阳", force = "汉",
            description = "外戚与宦官的生死博弈",
            initialEmperor = emperor,
            initialMinisters = ministers,
            initialConsorts = consorts,
            initialMemorials = memorials,
            initialPrefectures = prefs
        )
    }

    // ===================== 白袍·陈庆之剧本 =====================

    private fun baiqingchenqingzhi(): Scenario {
        val emperor = Emperor(
            name = "陈庆之", dynasty = "白袍", force = "白袍",
            capitalPrefecture = "洛阳",
            year = 1, month = 1, day = 1, shichen = 0,
            power = 85, intellect = 75, leadership = 90,
            benevolence = 80, authority = 60, charisma = 70
        )
        val ministers = listOf(
            m("chenqingzhi", "陈庆之", "大都督", "白袍", 88, 80, 92, 75, 80, 90),
            m("follower1", "义从甲", "校尉", "白袍", 70, 50, 60, 50, 70, 75),
            m("follower2", "义从乙", "校尉", "白袍", 65, 55, 58, 55, 70, 70)
        )
        val consorts = listOf(
            c("pingmin", "民女", 75, 60, "平民")
        )
        val memorials = listOf(
            memorialMilitary("陈庆之"),
            memorialPersonnel("义从甲"),
            memorialTax()
        )
        val prefs = defaultPrefectures().toMutableList().apply {
            this.find { it.id == "luoyang" }?.let {
                it.governor = "chenqingzhi"
                it.ownerForce = "白袍"
            }
        }
        return Scenario(
            id = "baiqing", name = "陈庆之", dynasty = "白袍",
            era = "白袍起兵", difficulty = "极难",
            capital = "洛阳", force = "白袍",
            description = "名师大将莫自牢，千兵万马避白袍",
            initialEmperor = emperor,
            initialMinisters = ministers,
            initialConsorts = consorts,
            initialMemorials = memorials,
            initialPrefectures = prefs
        )
    }

    // ===================== 工具方法 =====================

    private fun m(id: String, name: String, title: String, force: String,
                 power: Int, intellect: Int, leadership: Int, benevolence: Int,
                 loyalty: Int, relation: Int) = Minister(
        id, name, title, force, id,
        power, intellect, leadership, benevolence, loyalty, relation
    )

    private fun c(id: String, name: String, beauty: Int, talent: Int, family: String) =
        Consort(id, name, beauty, talent, family, favor = 40)

    private fun memorialTax() = Memorial(
        id = "mem_tax_${System.currentTimeMillis()}",
        type = MemorialType.TAX,
        title = "请增税以充国库",
        content = "今国库空虚，军饷欠发三月。恳请陛下暂增商税三成，以解燃眉之急。",
        suggestedAction = "增商税三成",
        ministerId = "xunyu",
        goldEffect = 3000, moraleEffect = -5
    )

    private fun memorialMilitary(ministerId: String) = Memorial(
        id = "mem_mil_${System.currentTimeMillis()}",
        type = MemorialType.MILITARY,
        title = "请增兵以镇边患",
        content = "边关告急，贼兵犯境。恳请陛下下诏增兵五千，以守疆土。",
        suggestedAction = "增兵五千",
        ministerId = ministerId,
        troopEffect = 5000, goldEffect = -1000
    )

    private fun memorialPersonnel(ministerId: String) = Memorial(
        id = "mem_per_${System.currentTimeMillis()}",
        type = MemorialType.PERSONNEL,
        title = "请擢升有功之士",
        content = "近有将士立下赫赫战功，恳请陛下擢升，以励来者。",
        suggestedAction = "擢升三人",
        ministerId = ministerId,
        loyaltyEffect = 5
    )

    private fun memorialJustice() = Memorial(
        id = "mem_jus_${System.currentTimeMillis()}",
        type = MemorialType.JUSTICE,
        title = "请严惩贪墨之徒",
        content = "近有官员贪墨赈灾钱粮，民怨沸腾。恳请陛下明正典刑。",
        suggestedAction = "抄没家产",
        ministerId = "liru",
        goldEffect = 1000, moraleEffect = 5
    )

    private fun memorialPublicWorks(ministerId: String) = Memorial(
        id = "mem_pub_${System.currentTimeMillis()}",
        type = MemorialType.PUBLIC_WORKS,
        title = "请修缮都城水渠",
        content = "都城水渠年久失修，连月大雨后多处溃堤。恳请拨银修缮。",
        suggestedAction = "拨银两千修缮",
        ministerId = ministerId,
        goldEffect = -2000, moraleEffect = 3
    )

    private fun memorialDiplomacy(ministerId: String) = Memorial(
        id = "mem_dip_${System.currentTimeMillis()}",
        type = MemorialType.DIPLOMACY,
        title = "请遣使结好邻邦",
        content = "邻国近来遣使示好，恳请陛下遣使回报，结为盟友。",
        suggestedAction = "遣使回访",
        ministerId = ministerId,
        goldEffect = -500, moraleEffect = 2
    )

    /**
     * 13 州 40 郡（硬编码坐标）。
     */
    private fun defaultPrefectures(): List<Prefecture> = listOf(
        // 司隶
        p("luoyang", "洛阳", "司隶", 12, 8, "群雄", 8000, 90),
        p("chang'an", "长安", "司隶", 8, 6, "群雄", 7000, 75),
        p("hongnong", "弘农", "司隶", 9, 7, "魏", 4000, 70),

        // 冀州
        p("ye", "邺", "冀州", 14, 7, "魏", 10000, 85),
        p("jizhou", "冀州", "冀州", 13, 6, "魏", 6000, 75),

        // 兖州
        p("chenliu", "陈留", "兖州", 13, 9, "魏", 6000, 80),
        p("yanzhou", "兖州", "兖州", 14, 8, "魏", 5000, 75),

        // 豫州
        p("xuchang", "许昌", "豫州", 13, 10, "魏", 8000, 85),
        p("ruzhou", "汝南", "豫州", 12, 12, "魏", 5000, 70),

        // 徐州
        p("xiaopei", "小沛", "徐州", 16, 9, "魏", 4000, 65),
        p("pengcheng", "彭城", "徐州", 17, 8, "吴", 5000, 70),
        p("langu", "琅琊", "徐州", 18, 7, "吴", 4000, 65),

        // 青州
        p("qingzhou", "青州", "青州", 17, 6, "吴", 6000, 75),
        p("beihai", "北海", "青州", 18, 5, "吴", 5000, 70),

        // 幽州
        p("youzhou", "幽州", "幽州", 17, 2, "匈奴", 7000, 70),
        p("zhuojun", "涿郡", "幽州", 16, 4, "魏", 5000, 75),

        // 并州
        p("bingzhou", "并州", "并州", 13, 3, "匈奴", 5000, 65),
        p("shangdang", "上党", "并州", 12, 5, "魏", 4000, 60),

        // 雍州
        p("yongzhou", "雍州", "雍州", 6, 6, "群雄", 5000, 70),
        p("tianshui", "天水", "雍州", 5, 7, "群雄", 4000, 65),

        // 凉州
        p("liangzhou", "凉州", "凉州", 3, 5, "群雄", 4000, 55),
        p("wuwei", "武威", "凉州", 2, 4, "群雄", 3000, 50),

        // 益州
        p("chengdu", "成都", "益州", 6, 13, "蜀", 12000, 90),
        p("yizhou", "益州", "益州", 8, 13, "蜀", 7000, 80),
        p("hanzhong", "汉中", "益州", 8, 10, "蜀", 6000, 70),
        p("yongan", "永安", "益州", 9, 15, "蜀", 5000, 65),

        // 荆州
        p("jingzhou", "荆州", "荆州", 11, 13, "蜀", 8000, 75),
        p("xiangyang", "襄阳", "荆州", 10, 11, "魏", 7000, 80),
        p("jiangling", "江陵", "荆州", 11, 14, "蜀", 6000, 75),
        p("wuling", "武陵", "荆州", 12, 16, "蜀", 4000, 65),
        p("changsha", "长沙", "荆州", 13, 17, "吴", 5000, 70),

        // 扬州
        p("jianye", "建业", "扬州", 18, 13, "吴", 10000, 85),
        p("wu", "吴郡", "扬州", 19, 14, "吴", 7000, 80),
        p("kuaiji", "会稽", "扬州", 20, 15, "吴", 5000, 75),
        p("lujiang", "庐江", "扬州", 15, 13, "吴", 5000, 70),

        // 交州
        p("jiaozhou", "交州", "交州", 16, 20, "吴", 4000, 55),
        p("nanhai", "南海", "交州", 17, 21, "吴", 4000, 60),
        p("jiuzhen", "九真", "交州", 17, 22, "吴", 3000, 55),

        // 关隘
        p("hangu", "函谷关", "关隘", 10, 7, "魏", 3000, 90),
        p("yanmen", "雁门关", "关隘", 14, 2, "匈奴", 3000, 85),
        p("wuguan", "武关", "关隘", 8, 9, "魏", 2500, 80),

        // 草原/边陲
        p("daijun", "代郡", "边陲", 15, 1, "匈奴", 2500, 50),
        p("longxi", "陇西", "边陲", 4, 6, "群雄", 3000, 55)
    )

    private fun p(id: String, name: String, region: String, gx: Int, gy: Int,
                 owner: String, troops: Int, morale: Int) = Prefecture(
        id, name, region, gx, gy,
        governor = null,
        troops = troops, morale = morale,
        tax = troops / 5, ownerForce = owner
    )
}