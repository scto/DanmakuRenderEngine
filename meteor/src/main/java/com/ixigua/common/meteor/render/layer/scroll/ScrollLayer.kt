package com.ixigua.common.meteor.render.layer.scroll

import android.graphics.Canvas
import android.view.MotionEvent
import com.ixigua.common.meteor.control.ConfigChangeListener
import com.ixigua.common.meteor.control.DanmakuConfig
import com.ixigua.common.meteor.control.DanmakuController
import com.ixigua.common.meteor.data.IDanmakuData
import com.ixigua.common.meteor.render.IRenderLayer
import com.ixigua.common.meteor.render.cache.IDrawCachePool
import com.ixigua.common.meteor.render.draw.IDrawItem
import com.ixigua.common.meteor.touch.ITouchDelegate
import com.ixigua.common.meteor.touch.ITouchTarget
import com.ixigua.common.meteor.utils.LAYER_TYPE_SCROLL
import java.util.*

/**
 * Created by dss886 on 2018/11/8.
 */
class ScrollLayer(private val mController: DanmakuController,
                  private val mCachePool: IDrawCachePool) : IRenderLayer, ITouchDelegate, ConfigChangeListener {

    private val mScrollLines = mutableListOf<ScrollLine>()
    private val mPreDrawItems = LinkedList<IDrawItem<IDanmakuData>>()
    private val mBufferItems = LinkedList<IDrawItem<IDanmakuData>>()
    private var mWidth = 0
    private var mHeight = 0

    init {
        mController.config.addListener(this)
    }

    override fun getLayerType(): Int {
        return LAYER_TYPE_SCROLL
    }

    override fun onLayoutSizeChanged(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        configScrollLine()
    }

    override fun addItems(list: List<IDrawItem<IDanmakuData>>) {
        mBufferItems.addAll(list)
        val iterator = mBufferItems.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (addItemImpl(next)) {
                iterator.remove()
            }
        }
        trimBuffer()
    }

    /**
     * Try add item to lines.
     * Return true if find a line has enough space to add, return false otherwise.
     */
    private fun addItemImpl(item: IDrawItem<IDanmakuData>): Boolean {
        mScrollLines.forEach { line ->
            if (line.addItem(item)) {
                return true
            }
        }
        return false
    }

    override fun typesetting(isPlaying: Boolean, configChanged: Boolean) {
        mScrollLines.forEach { line ->
            line.typesetting(isPlaying, configChanged)
        }
        if (configChanged) {
            mBufferItems.forEach { it.measure(mController.config) }
        }
    }

    override fun drawLayoutBounds(canvas: Canvas) {
        mScrollLines.forEach { line ->
            line.drawLayoutBounds(canvas)
        }
    }

    override fun getPreDrawItems(): List<IDrawItem<IDanmakuData>> {
        mPreDrawItems.clear()
        mScrollLines.forEach { line ->
            mPreDrawItems.addAll(line.getPreDrawItems())
        }
        return mPreDrawItems
    }

    override fun findTouchTarget(event: MotionEvent): ITouchTarget? {
        mScrollLines.forEach { line ->
            if (event.y > line.y + line.height) {
                return@forEach
            }
            if (event.y >= line.y) {
                return line
            }
            return null
        }
        return null
    }

    override fun clear() {
        mScrollLines.forEach { line ->
            line.clearRender()
        }
    }

    override fun onConfigChanged(type: Int) {
        when (type) {
            DanmakuConfig.TYPE_SCROLL_LINE_HEIGHT,
            DanmakuConfig.TYPE_SCROLL_LINE_COUNT,
            DanmakuConfig.TYPE_SCROLL_LINE_SPACE,
            DanmakuConfig.TYPE_SCROLL_MARGIN_TOP -> configScrollLine()
        }
    }

    private fun configScrollLine() {
        val config = mController.config
        val lineCount = config.scroll.lineCount
        val lineHeight = config.scroll.lineHeight
        val lineSpace = config.scroll.lineSpace
        val marginTop = config.scroll.marginTop
        if (lineCount > mScrollLines.size) {
            for (i in 1..(lineCount - mScrollLines.size)) {
                mScrollLines.add(ScrollLine(mController, mCachePool).apply {
                    mController.registerCmdMonitor(this)
                })
            }
        } else if (lineCount < mScrollLines.size) {
            val subLines = mScrollLines.take(lineCount)
            mScrollLines.takeLast(mScrollLines.size - lineCount).forEach {
                mController.unRegisterCmdMonitor(it)
            }
            mScrollLines.clear()
            mScrollLines.addAll(subLines)
        }
        mScrollLines.forEachIndexed { index, line ->
            line.onLayoutChanged(mWidth.toFloat(), lineHeight, marginTop + index * (lineSpace + lineHeight))
        }
    }

    private fun trimBuffer() {
        if (mBufferItems.isEmpty() || mBufferItems.size <= mController.config.common.typesetBufferSize) {
            return
        }
        while (mBufferItems.size > mController.config.common.typesetBufferSize) {
            val item = mBufferItems.minBy {
                @Suppress("UNCHECKED_CAST")
                mController.config.common.bufferDiscardRule.invoke(it.data) as Comparable<Any?>
            }
            item?.let {
                mBufferItems.remove(it)
                mCachePool.release(it)
            }
        }
    }
}
