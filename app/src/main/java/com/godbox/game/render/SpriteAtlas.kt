package com.godbox.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.godbox.game.engine.EntityType
import com.godbox.game.engine.TerrainType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sprite 缓存：启动时一次性生成所有地形、生物的位图。运行期仅 drawBitmap。
 *
 * 每个 sprite 的画布坐标约定：
 * - terrainGrass 这种：以"菱形左尖"为 (0, 0)，向右下生长
 * - tileWidth / tileHeight 控制菱形大小
 * - height 是方块的垂直像素（Z 方向）
 *
 * 实际渲染时调用 atlas.drawTerrain(canvas, terrain, screenX, screenY, options)
 */
class SpriteAtlas(val tileWidth: Int = 64, val tileHeight: Int = 32, val blockHeight: Int = 24) {

    // terrainType -> 顶面 / 前面 / 右侧面 3 个 bitmap
    data class TerrainSprites(
        val top: Bitmap,
        val front: Bitmap,
        val right: Bitmap
    )

    val terrainSprites: Map<TerrainType, TerrainSprites> = buildMap {
        for (tt in TerrainType.values()) {
            put(tt, createTerrainSprites(tt))
        }
    }

    // 装饰物（树/房屋/山顶雪冠）单独的 bitmap
    val treeTop: Bitmap = createTreeTop()
    val treeTrunk: Bitmap = createTreeTrunk()
    val houseBody: Bitmap = createHouseBody()
    val roofLeft: Bitmap = createRoofLeft()
    val roofRight: Bitmap = createRoofRight()

    // 生物位图（32x48 单位：32 宽 48 高，包括身体+头+腿）
    data class EntitySprites(
        val body: Bitmap,        // 全身图，朝右
        val bodyLeft: Bitmap,    // 全身图，朝左
        val walkFrames: Int = 2,
        val walkOffsetY: IntArray = intArrayOf(0, 1) // 走路时身体上下抖动
    )

    val humanSprites: EntitySprites = createHumanSprites()
    val sheepSprites: EntitySprites = createSheepSprites()
    val wolfSprites: EntitySprites = createWolfSprites()
    val fishSprites: EntitySprites = createFishSprites()

    // 文明/选中格高亮
    val selectDiamond: Bitmap = createSelectDiamond()

    // ========================================================================
    // 地形 sprite 生成
    // ========================================================================

    private fun createTerrainSprites(tt: TerrainType): TerrainSprites {
        val top = createTerrainTop(tt)
        val front = createTerrainFront(tt)
        val right = createTerrainRight(tt)
        return TerrainSprites(top, front, right)
    }

    /** 顶面 bitmap：菱形，加纹理噪点 */
    private fun createTerrainTop(tt: TerrainType): Bitmap {
        val w = tileWidth; val h = tileHeight
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val baseColor = tt.topColor

        // 底色
        paint.color = baseColor
        val path = Path()
        path.moveTo(0f, h / 2f)
        path.lineTo(w / 2f, 0f)
        path.lineTo(w.toFloat(), h / 2f)
        path.lineTo(w / 2f, h.toFloat())
        path.close()
        c.drawPath(path, paint)

        // 纹理噪点 + 渐变
        when (tt) {
            TerrainType.GRASS -> addGrassTexture(c, w, h)
            TerrainType.WATER -> addWaterTexture(c, w, h)
            TerrainType.SAND -> addSandTexture(c, w, h)
            TerrainType.DIRT -> addDirtTexture(c, w, h)
            TerrainType.MOUNTAIN -> addStoneTexture(c, w, h)
            TerrainType.TREE -> addGrassTexture(c, w, h)  // 树的地基是草地
            TerrainType.VILLAGE -> addDirtTexture(c, w, h)
            TerrainType.ROAD -> addRoadTexture(c, w, h)
            TerrainType.ASH -> addAshTexture(c, w, h)
        }

        // 菱形深色描边
        paint.color = darken(baseColor, 0.35f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    /** 前面 bitmap：菱形+方块高度 */
    private fun createTerrainFront(tt: TerrainType): Bitmap {
        val w = tileWidth
        val totalH = tileHeight + blockHeight
        val bmp = Bitmap.createBitmap(w, totalH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val baseColor = tt.frontColor
        val h = tileHeight
        val bh = blockHeight

        // 前面菱形
        val path = Path()
        path.moveTo(0f, h / 2f)
        path.lineTo(w / 2f, h.toFloat())
        path.lineTo(w.toFloat(), h / 2f)
        path.lineTo(w / 2f, h + bh.toFloat())
        path.close()
        paint.color = baseColor
        c.drawPath(path, paint)

        // 纹理噪点
        when (tt) {
            TerrainType.MOUNTAIN -> addStoneTexture(c, w, totalH)
            TerrainType.GRASS -> addGrassTexture(c, w, totalH)
            TerrainType.DIRT -> addDirtTexture(c, w, totalH)
            TerrainType.SAND -> addSandTexture(c, w, totalH)
            TerrainType.TREE -> addGrassTexture(c, w, totalH)
            TerrainType.WATER -> addWaterTexture(c, w, totalH)
            else -> {}
        }

        // 描边
        paint.color = darken(baseColor, 0.30f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    /** 右侧面 bitmap */
    private fun createTerrainRight(tt: TerrainType): Bitmap {
        val w = tileWidth
        val totalH = tileHeight + blockHeight
        val bmp = Bitmap.createBitmap(w, totalH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val baseColor = tt.rightColor
        val h = tileHeight
        val bh = blockHeight

        // 右侧面：右尖为起点
        val path = Path()
        path.moveTo(w / 2f, 0f)
        path.lineTo(w.toFloat(), h / 2f)
        path.lineTo(w / 2f, h + bh.toFloat())
        path.lineTo(w / 2f, h.toFloat())
        path.close()
        paint.color = baseColor
        c.drawPath(path, paint)

        // 描边
        paint.color = darken(baseColor, 0.35f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    private fun addGrassTexture(c: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 草丛噪点
        for (i in 0 until 60) {
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            paint.color = listOf(
                Color.rgb(70, 130, 55),
                Color.rgb(95, 165, 70),
                Color.rgb(60, 110, 45),
                Color.rgb(110, 175, 80)
            ).random()
            c.drawRect(x, y, x + 1.5f, y + 2.5f, paint)
        }
        // 几片大草叶
        for (i in 0 until 8) {
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            paint.color = Color.rgb(75, 145, 55)
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            c.drawLine(x, y, x + 1f, y - 4f, paint)
            c.drawLine(x + 1f, y, x + 2f, y - 3f, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun addWaterTexture(c: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 蓝色波纹
        for (i in 0 until 10) {
            val y = (Random.nextFloat() * h).coerceAtLeast(2f)
            val x1 = Random.nextFloat() * (w * 0.3f)
            val x2 = x1 + 8f + Random.nextFloat() * 12f
            paint.color = Color.argb(140, 180, 215, 255)
            paint.strokeWidth = 1.5f
            paint.style = Paint.Style.STROKE
            val path = Path()
            path.moveTo(x1, y)
            path.quadTo((x1 + x2) / 2f, y - 2f, x2, y)
            c.drawPath(path, paint)
        }
        // 暗色阴影
        paint.style = Paint.Style.FILL
        for (i in 0 until 30) {
            paint.color = Color.argb(80, 30, 60, 130)
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            c.drawRect(x, y, x + 2f, y + 1f, paint)
        }
    }

    private fun addSandTexture(c: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 沙粒
        for (i in 0 until 100) {
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            val v = Random.nextInt(-30, 30)
            paint.color = Color.rgb(
                (Color.red(Color.rgb(225, 200, 130)) + v).coerceIn(0, 255),
                (Color.green(Color.rgb(225, 200, 130)) + v).coerceIn(0, 255),
                (Color.blue(Color.rgb(225, 200, 130)) + v).coerceIn(0, 255)
            )
            c.drawRect(x, y, x + 1f, y + 1f, paint)
        }
    }

    private fun addDirtTexture(c: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until 70) {
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            paint.color = Color.rgb(110 + Random.nextInt(30), 80 + Random.nextInt(20), 50 + Random.nextInt(15))
            c.drawRect(x, y, x + 1.5f, y + 1.5f, paint)
        }
        // 小石头
        for (i in 0 until 5) {
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            paint.color = Color.rgb(80, 70, 60)
            c.drawCircle(x, y, 1.5f, paint)
        }
    }

    private fun addStoneTexture(c: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 石头不规则纹理
        for (i in 0 until 80) {
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            val v = Random.nextInt(-25, 25)
            paint.color = Color.rgb(
                (120 + v).coerceIn(0, 255),
                (110 + v).coerceIn(0, 255),
                (100 + v).coerceIn(0, 255)
            )
            c.drawRect(x, y, x + 2f, y + 2f, paint)
        }
        // 裂缝
        paint.color = Color.rgb(60, 55, 50)
        paint.strokeWidth = 0.8f
        paint.style = Paint.Style.STROKE
        for (i in 0 until 3) {
            val x1 = Random.nextFloat() * w
            val y1 = Random.nextFloat() * h
            val x2 = x1 + (Random.nextFloat() * 10f - 5f)
            val y2 = y1 + (Random.nextFloat() * 6f - 3f)
            c.drawLine(x1, y1, x2, y2, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun addRoadTexture(c: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(110, 95, 75)
        for (i in 0 until 50) {
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            c.drawRect(x, y, x + 1.5f, y + 1.5f, paint)
        }
    }

    private fun addAshTexture(c: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until 80) {
            val x = Random.nextFloat() * w
            val y = Random.nextFloat() * h
            paint.color = Color.argb(180, Random.nextInt(40, 90), Random.nextInt(40, 80), Random.nextInt(40, 70))
            c.drawRect(x, y, x + 1f, y + 1f, paint)
        }
    }

    // ========================================================================
    // 装饰物（树/房子/雪冠）
    // ========================================================================

    private fun createTreeTop(): Bitmap {
        val sz = tileWidth + 16
        val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val cx = sz / 2f
        val cy = sz / 2f

        // 多层树冠（圆形叠加）
        // 底层暗
        paint.color = Color.rgb(35, 90, 35)
        c.drawCircle(cx, cy + 4, sz * 0.42f, paint)
        paint.color = Color.rgb(45, 105, 45)
        c.drawCircle(cx - 6, cy + 2, sz * 0.36f, paint)
        paint.color = Color.rgb(45, 105, 45)
        c.drawCircle(cx + 6, cy + 2, sz * 0.36f, paint)
        // 中层
        paint.color = Color.rgb(60, 130, 55)
        c.drawCircle(cx, cy - 4, sz * 0.40f, paint)
        // 高光
        paint.color = Color.rgb(85, 160, 75)
        c.drawCircle(cx - 4, cy - 8, sz * 0.22f, paint)
        paint.color = Color.rgb(120, 190, 100)
        c.drawCircle(cx - 6, cy - 10, sz * 0.10f, paint)

        // 边缘树叶小点
        paint.color = Color.rgb(45, 100, 45)
        for (i in 0 until 30) {
            val a = Random.nextFloat() * 6.28f
            val r = sz * (0.35f + Random.nextFloat() * 0.10f)
            val x = cx + cos(a) * r
            val y = cy + sin(a) * r
            c.drawCircle(x, y, 2.5f, paint)
        }
        return bmp
    }

    private fun createTreeTrunk(): Bitmap {
        val w = 12; val h = 18
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        // 树干深浅条纹
        paint.color = Color.rgb(80, 50, 25)
        c.drawRect(2f, 0f, w - 2f, h.toFloat(), paint)
        paint.color = Color.rgb(110, 75, 40)
        c.drawRect(4f, 0f, w - 4f, h.toFloat(), paint)
        paint.color = Color.rgb(140, 95, 50)
        c.drawRect(4f, 0f, 7f, h.toFloat(), paint)
        // 木纹
        paint.color = Color.rgb(60, 35, 15)
        for (i in 0 until 3) {
            val y = Random.nextFloat() * h
            c.drawLine(2f, y, w - 2f, y + 1, paint)
        }
        return bmp
    }

    private fun createHouseBody(): Bitmap {
        val w = tileWidth - 8
        val h = blockHeight + 4
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        // 黄色墙体
        paint.color = Color.rgb(230, 220, 150)
        c.drawRect(0f, 4f, w.toFloat(), h.toFloat(), paint)
        // 纹理：砖
        paint.color = Color.rgb(190, 175, 120)
        for (i in 0 until 6) {
            c.drawLine(0f, 4f + i * 4f, w.toFloat(), 4f + i * 4f, paint)
        }
        for (i in 0 until 7) {
            c.drawLine(i * (w / 6f), 4f, i * (w / 6f), h.toFloat(), paint)
        }
        // 门
        paint.color = Color.rgb(80, 50, 30)
        c.drawRect(w / 2f - 4f, h - 12f, w / 2f + 4f, h.toFloat(), paint)
        paint.color = Color.rgb(60, 40, 20)
        c.drawCircle(w / 2f + 2f, h - 6f, 1.2f, paint)
        // 窗
        paint.color = Color.rgb(180, 200, 220)
        c.drawRect(4f, 8f, 10f, 14f, paint)
        c.drawRect(w - 10f, 8f, w - 4f, 14f, paint)
        paint.color = Color.rgb(100, 80, 50)
        c.drawRect(4f, 8f, 10f, 14f, paint.apply { style = Paint.Style.STROKE; strokeWidth = 1f })
        paint.style = Paint.Style.FILL
        return bmp
    }

    private fun createRoofLeft(): Bitmap {
        val w = tileWidth
        val h = 16
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        // 三角屋顶（前面）
        val path = Path()
        path.moveTo(0f, h.toFloat())
        path.lineTo(w / 2f, 0f)
        path.lineTo(w / 2f, h.toFloat())
        path.close()
        paint.color = Color.rgb(190, 70, 60)
        c.drawPath(path, paint)
        // 高光
        paint.color = Color.rgb(220, 90, 80)
        val path2 = Path()
        path2.moveTo(0f, h.toFloat())
        path2.lineTo(w / 2f, 0f)
        path2.lineTo(w * 0.3f, h * 0.4f)
        path2.close()
        c.drawPath(path2, paint)
        // 瓦片纹
        paint.color = Color.rgb(130, 40, 35)
        for (i in 0 until 4) {
            c.drawLine(w * 0.1f + i * (w * 0.1f), h * 0.4f, w * 0.4f + i * (w * 0.05f), h * 0.1f, paint)
        }
        return bmp
    }

    private fun createRoofRight(): Bitmap {
        val w = tileWidth
        val h = 16
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        // 暗侧
        val path = Path()
        path.moveTo(w / 2f, 0f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(w / 2f, h.toFloat())
        path.close()
        paint.color = Color.rgb(150, 50, 45)
        c.drawPath(path, paint)
        return bmp
    }

    // ========================================================================
    // 生物 sprite
    // ========================================================================

    private fun createHumanSprites(): EntitySprites {
        val w = 24
        val h = 36
        val right = createSingleHuman(w, h, false)
        val left = createSingleHuman(w, h, true)
        return EntitySprites(right, left, walkFrames = 2,
            walkOffsetY = intArrayOf(0, 1))
    }

    private fun createSingleHuman(w: Int, h: Int, facingLeft: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // 腿
        val legColor = Color.rgb(80, 60, 40)
        c.drawRect(7f, h - 12f, 11f, h - 2f, paint)
        c.drawRect(13f, h - 12f, 17f, h - 2f, paint)
        paint.color = Color.rgb(60, 45, 30)
        c.drawRect(7f, h - 4f, 11f, h - 2f, paint)
        c.drawRect(13f, h - 4f, 17f, h - 2f, paint)

        // 鞋
        paint.color = Color.rgb(40, 30, 20)
        c.drawRect(6f, h - 2f, 11f, h.toFloat(), paint)
        c.drawRect(13f, h - 2f, 18f, h.toFloat(), paint)

        // 身体
        paint.color = Color.rgb(50, 90, 170)
        c.drawRect(6f, h - 24f, 18f, h - 12f, paint)
        // 衣服纹理
        paint.color = Color.rgb(40, 70, 140)
        c.drawLine(12f, h - 24f, 12f, h - 12f, paint)
        paint.color = Color.rgb(80, 120, 200)
        c.drawRect(8f, h - 22f, 10f, h - 20f, paint) // 扣子

        // 手臂
        paint.color = Color.rgb(240, 200, 160)
        c.drawRect(4f, h - 24f, 6f, h - 14f, paint)
        c.drawRect(18f, h - 24f, 20f, h - 14f, paint)

        // 脖子
        paint.color = Color.rgb(230, 195, 155)
        c.drawRect(10f, h - 27f, 14f, h - 24f, paint)

        // 头
        paint.color = Color.rgb(255, 220, 180)
        c.drawCircle(12f, h - 30f, 5f, paint)

        // 头发（黑）
        paint.color = Color.rgb(40, 30, 25)
        c.drawRect(7f, h - 35f, 17f, h - 32f, paint)
        c.drawCircle(12f, h - 33f, 5f, paint)
        // 头发上面
        paint.color = Color.rgb(50, 40, 30)
        c.drawRect(8f, h - 36f, 16f, h - 33f, paint)

        // 眼睛（左右不同方向）
        paint.color = Color.BLACK
        if (facingLeft) {
            c.drawRect(9f, h - 30f, 11f, h - 29f, paint)
        } else {
            c.drawRect(13f, h - 30f, 15f, h - 29f, paint)
        }

        // 嘴
        paint.color = Color.rgb(150, 80, 70)
        c.drawRect(11f, h - 28f, 13f, h - 27f, paint)

        // 描边
        paint.color = Color.argb(80, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        c.drawRect(6f, h - 24f, 18f, h - 12f, paint)
        c.drawCircle(12f, h - 30f, 5f, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    private fun createSheepSprites(): EntitySprites {
        val w = 30
        val h = 26
        val right = createSingleSheep(w, h, false)
        val left = createSingleSheep(w, h, true)
        return EntitySprites(right, left)
    }

    private fun createSingleSheep(w: Int, h: Int, facingLeft: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // 4 条腿
        paint.color = Color.rgb(60, 50, 40)
        c.drawRect(7f, h - 8f, 10f, h.toFloat(), paint)
        c.drawRect(13f, h - 8f, 16f, h.toFloat(), paint)
        c.drawRect(19f, h - 8f, 22f, h.toFloat(), paint)
        c.drawRect(25f, h - 8f, 28f, h.toFloat(), paint)

        // 身体（蓬松白色椭圆）
        paint.color = Color.rgb(245, 245, 240)
        c.drawOval(4f, h - 18f, w - 4f, h - 6f, paint)
        // 卷毛纹理
        paint.color = Color.rgb(220, 220, 215)
        for (i in 0 until 8) {
            val cx = 6f + i * 3f + Random.nextFloat()
            val cy = h - 14f + Random.nextFloat() * 4f
            c.drawCircle(cx, cy, 2f, paint)
        }
        // 高光
        paint.color = Color.rgb(255, 255, 250)
        c.drawOval(8f, h - 17f, 14f, h - 13f, paint)

        // 头（米色）
        val headX = if (facingLeft) 4f else w - 12f
        paint.color = Color.rgb(160, 130, 90)
        c.drawCircle(headX + 4f, h - 14f, 4f, paint)
        // 耳朵
        paint.color = Color.rgb(140, 110, 75)
        c.drawOval(headX + 1f, h - 18f, headX + 4f, h - 15f, paint)
        c.drawOval(headX + 4f, h - 18f, headX + 7f, h - 15f, paint)
        // 眼睛
        paint.color = Color.BLACK
        val eyeX = if (facingLeft) headX + 2f else headX + 6f
        c.drawCircle(eyeX, h - 14f, 0.8f, paint)

        // 描边
        paint.color = Color.argb(80, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        c.drawOval(4f, h - 18f, w - 4f, h - 6f, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    private fun createWolfSprites(): EntitySprites {
        val w = 32
        val h = 28
        val right = createSingleWolf(w, h, false)
        val left = createSingleWolf(w, h, true)
        return EntitySprites(right, left)
    }

    private fun createSingleWolf(w: Int, h: Int, facingLeft: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // 4 条腿
        paint.color = Color.rgb(50, 45, 40)
        c.drawRect(8f, h - 10f, 11f, h.toFloat(), paint)
        c.drawRect(14f, h - 10f, 17f, h.toFloat(), paint)
        c.drawRect(20f, h - 10f, 23f, h.toFloat(), paint)
        c.drawRect(26f, h - 10f, 29f, h.toFloat(), paint)

        // 尾巴
        paint.color = Color.rgb(80, 70, 65)
        val tailPath = Path()
        if (facingLeft) {
            tailPath.moveTo(w - 4f, h - 16f)
            tailPath.lineTo(w - 1f, h - 18f)
            tailPath.lineTo(w - 2f, h - 12f)
            tailPath.close()
        } else {
            tailPath.moveTo(4f, h - 16f)
            tailPath.lineTo(1f, h - 18f)
            tailPath.lineTo(2f, h - 12f)
            tailPath.close()
        }
        c.drawPath(tailPath, paint)

        // 身体
        paint.color = Color.rgb(100, 90, 85)
        c.drawOval(4f, h - 18f, w - 4f, h - 8f, paint)
        // 身体纹理
        paint.color = Color.rgb(80, 70, 65)
        for (i in 0 until 5) {
            val x = 6f + i * 5f
            c.drawLine(x, h - 16f, x + 2f, h - 10f, paint)
        }

        // 头
        val headX = if (facingLeft) 4f else w - 16f
        paint.color = Color.rgb(100, 90, 85)
        c.drawOval(headX, h - 18f, headX + 14f, h - 8f, paint)

        // 耳朵
        paint.color = Color.rgb(80, 70, 65)
        val earPath = Path()
        earPath.moveTo(headX + 2f, h - 16f)
        earPath.lineTo(headX + 3f, h - 22f)
        earPath.lineTo(headX + 5f, h - 16f)
        earPath.close()
        c.drawPath(earPath, paint)
        val earPath2 = Path()
        earPath2.moveTo(headX + 9f, h - 16f)
        earPath2.lineTo(headX + 11f, h - 22f)
        earPath2.lineTo(headX + 12f, h - 16f)
        earPath2.close()
        c.drawPath(earPath2, paint)
        // 耳内
        paint.color = Color.rgb(120, 80, 80)
        c.drawCircle(headX + 3f, h - 18f, 1f, paint)
        c.drawCircle(headX + 11f, h - 18f, 1f, paint)

        // 眼睛（红色）
        paint.color = Color.rgb(220, 80, 50)
        val eyeX = if (facingLeft) headX + 3f else headX + 9f
        c.drawCircle(eyeX, h - 13f, 1.2f, paint)
        paint.color = Color.BLACK
        c.drawCircle(eyeX, h - 13f, 0.4f, paint)

        // 嘴（黑尖）
        val noseX = if (facingLeft) headX - 1f else headX + 14f
        paint.color = Color.BLACK
        val nosePath = Path()
        nosePath.moveTo(noseX, h - 11f)
        nosePath.lineTo(noseX + (if (facingLeft) -2f else 2f), h - 10f)
        nosePath.lineTo(noseX, h - 9f)
        nosePath.close()
        c.drawPath(nosePath, paint)

        // 描边
        paint.color = Color.argb(100, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        c.drawOval(4f, h - 18f, w - 4f, h - 8f, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    private fun createFishSprites(): EntitySprites {
        val w = 18
        val h = 10
        val right = createSingleFish(w, h, false)
        val left = createSingleFish(w, h, true)
        return EntitySprites(right, left)
    }

    private fun createSingleFish(w: Int, h: Int, facingLeft: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // 身体（椭圆）
        paint.color = Color.rgb(60, 130, 200)
        c.drawOval(2f, 1f, w - 6f, h - 1f, paint)
        // 鱼鳞纹理
        paint.color = Color.rgb(80, 150, 220)
        for (i in 0 until 3) {
            for (j in 0 until 2) {
                val x = 4f + i * 3f
                val y = 2f + j * 4f
                c.drawCircle(x, y, 0.8f, paint)
            }
        }
        // 腹部浅
        paint.color = Color.rgb(140, 180, 220)
        c.drawOval(3f, h - 4f, w - 8f, h - 1f, paint)

        // 尾（三角）
        val tailPath = Path()
        if (facingLeft) {
            tailPath.moveTo(w - 6f, h / 2f)
            tailPath.lineTo(w.toFloat(), 0f)
            tailPath.lineTo(w.toFloat(), h.toFloat())
            tailPath.close()
        } else {
            tailPath.moveTo(6f, h / 2f)
            tailPath.lineTo(0f, 0f)
            tailPath.lineTo(0f, h.toFloat())
            tailPath.close()
        }
        paint.color = Color.rgb(40, 90, 160)
        c.drawPath(tailPath, paint)

        // 眼睛
        paint.color = Color.WHITE
        val eyeX = if (facingLeft) 4f else w - 9f
        c.drawCircle(eyeX, h / 2f - 1f, 1.5f, paint)
        paint.color = Color.BLACK
        c.drawCircle(eyeX, h / 2f - 1f, 0.8f, paint)

        // 描边
        paint.color = Color.argb(80, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        c.drawOval(2f, 1f, w - 6f, h - 1f, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    // ========================================================================
    // 选中格
    // ========================================================================

    private fun createSelectDiamond(): Bitmap {
        val w = tileWidth + 8
        val h = tileHeight + 16
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val cx = w / 2f
        val cy = h / 2f
        val rx = (tileWidth / 2f) + 2f
        val ry = (tileHeight / 2f) + 4f
        paint.color = Color.argb(220, 255, 255, 100)
        paint.strokeWidth = 2.5f
        c.drawLine(cx - rx, cy, cx, cy - ry, paint)
        c.drawLine(cx, cy - ry, cx + rx, cy, paint)
        c.drawLine(cx + rx, cy, cx, cy + ry, paint)
        c.drawLine(cx, cy + ry, cx - rx, cy, paint)
        return bmp
    }

    private fun darken(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - factor)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    fun recycle() {
        terrainSprites.values.forEach {
            it.top.recycle()
            it.front.recycle()
            it.right.recycle()
        }
        treeTop.recycle()
        treeTrunk.recycle()
        houseBody.recycle()
        roofLeft.recycle()
        roofRight.recycle()
        humanSprites.body.recycle()
        humanSprites.bodyLeft.recycle()
        sheepSprites.body.recycle()
        sheepSprites.bodyLeft.recycle()
        wolfSprites.body.recycle()
        wolfSprites.bodyLeft.recycle()
        fishSprites.body.recycle()
        fishSprites.bodyLeft.recycle()
        selectDiamond.recycle()
    }
}