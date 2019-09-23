package com.ixigua.common.meteor.render.draw.text

import com.ixigua.common.meteor.data.DanmakuData
import com.ixigua.common.meteor.render.draw.DrawItem
import com.ixigua.common.meteor.render.draw.IDrawItemFactory
import com.ixigua.common.meteor.utils.DRAW_TYPE_TEXT

/**
 * Created by dss886 on 2019-05-19.
 */
class TextDrawItemFactory: IDrawItemFactory {

    override fun getDrawType(): Int {
        return DRAW_TYPE_TEXT
    }

    override fun generateDrawItem(): DrawItem<out DanmakuData> {
        return TextDrawItem()
    }

}