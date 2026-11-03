package com.unscroll.block

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

data class TextNode(val text: String, val bounds: Rect)

fun AccessibilityNodeInfo.hasAny(ids: List<String>): Boolean =
    ids.any { id -> findAccessibilityNodeInfosByViewId(id).any { it.isVisibleToUser } }

fun AccessibilityNodeInfo.firstText(ids: List<String>): String? =
    ids.firstNotNullOfOrNull { id ->
        findAccessibilityNodeInfosByViewId(id).firstNotNullOfOrNull { n -> n.text?.toString()?.takeIf { it.isNotBlank() } }
    }

fun AccessibilityNodeInfo.firstBounds(ids: List<String>): Rect? =
    ids.firstNotNullOfOrNull { id ->
        findAccessibilityNodeInfosByViewId(id).firstOrNull { it.isVisibleToUser }?.let { n -> Rect().also(n::getBoundsInScreen) }
    }

/** Breadth-first walk collecting visible text (or content descriptions), visiting at most [limit] nodes. */
fun AccessibilityNodeInfo.visibleTexts(limit: Int): List<TextNode> {
    val out = ArrayList<TextNode>()
    val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(this@visibleTexts) }
    var visited = 0
    while (queue.isNotEmpty() && visited < limit) {
        val node = queue.removeFirst()
        visited++
        if (!node.isVisibleToUser) continue
        val text = node.text?.toString()?.takeIf { it.isNotBlank() } ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank()) out.add(TextNode(text, Rect().also(node::getBoundsInScreen)))
        for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
    }
    return out
}
