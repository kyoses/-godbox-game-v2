package com.threekingdoms.emperor.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.threekingdoms.emperor.R

/**
 * 底部行动菜单：5 个主功能按钮。
 */
class ActionMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onCouncil()
        fun onAudience()
        fun onMap()
        fun onCampaign()
        fun onHarem()
    }

    var listener: Listener? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setBackgroundColor(0xFF1B1B1B.toInt())
        setPadding(0, 8, 0, 16)

        addView(makeBtn(R.string.council) { listener?.onCouncil() })
        addView(makeBtn(R.string.audience) { listener?.onAudience() })
        addView(makeBtn(R.string.map_view) { listener?.onMap() })
        addView(makeBtn(R.string.campaign) { listener?.onCampaign() })
        addView(makeBtn(R.string.harem) { listener?.onHarem() })
    }

    private fun makeBtn(stringId: Int, onClick: () -> Unit): View {
        val btn = Button(context).apply {
            text = context.getString(stringId)
            textSize = 14f
            setBackgroundResource(R.drawable.btn_round_gold)
            setTextColor(0xFFC9A14A.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 4; marginEnd = 4
            }
            setOnClickListener { onClick() }
        }
        return btn
    }
}