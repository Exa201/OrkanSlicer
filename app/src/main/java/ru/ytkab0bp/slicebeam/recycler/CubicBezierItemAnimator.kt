/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.ytkab0bp.slicebeam.recycler

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** RecyclerView row animations using the app's cubic-bezier interpolator. */
class CubicBezierItemAnimator : SimpleItemAnimator() {
    private data class MoveInfo(
        val holder: RecyclerView.ViewHolder,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
    )

    private class ChangeInfo(
        var oldHolder: RecyclerView.ViewHolder?,
        var newHolder: RecyclerView.ViewHolder?,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
    )

    private val pendingRemovals = ArrayList<RecyclerView.ViewHolder>()
    private val pendingAdditions = ArrayList<RecyclerView.ViewHolder>()
    private val pendingMoves = ArrayList<MoveInfo>()
    private val pendingChanges = ArrayList<ChangeInfo>()

    private val additionsList = ArrayList<ArrayList<RecyclerView.ViewHolder>>()
    private val movesList = ArrayList<ArrayList<MoveInfo>>()
    private val changesList = ArrayList<ArrayList<ChangeInfo>>()

    private val addAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val moveAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val removeAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val changeAnimations = ArrayList<RecyclerView.ViewHolder>()

    init {
        setAddDuration(ANIMATION_DURATION)
        setRemoveDuration(ANIMATION_DURATION)
    }

    override fun runPendingAnimations() {
        val removalsPending = pendingRemovals.isNotEmpty()
        val movesPending = pendingMoves.isNotEmpty()
        val changesPending = pendingChanges.isNotEmpty()
        val additionsPending = pendingAdditions.isNotEmpty()
        if (!removalsPending && !movesPending && !additionsPending && !changesPending) return

        pendingRemovals.forEach(::animateRemoveImpl)
        pendingRemovals.clear()

        if (movesPending) {
            val moves = ArrayList<MoveInfo>(pendingMoves)
            movesList.add(moves)
            pendingMoves.clear()
            val mover = Runnable {
                moves.forEach { animateMoveImpl(it.holder, it.fromX, it.fromY, it.toX, it.toY) }
                moves.clear()
                movesList.remove(moves)
            }
            if (removalsPending) {
                ViewCompat.postOnAnimationDelayed(moves.first().holder.itemView, mover, removeDuration)
            } else {
                mover.run()
            }
        }

        if (changesPending) {
            val changes = ArrayList<ChangeInfo>(pendingChanges)
            changesList.add(changes)
            pendingChanges.clear()
            val changer = Runnable {
                changes.forEach(::animateChangeImpl)
                changes.clear()
                changesList.remove(changes)
            }
            if (removalsPending) {
                val holder = changes.first().oldHolder ?: changes.first().newHolder
                holder?.let { ViewCompat.postOnAnimationDelayed(it.itemView, changer, removeDuration) }
                    ?: changer.run()
            } else {
                changer.run()
            }
        }

        if (additionsPending) {
            val additions = ArrayList<RecyclerView.ViewHolder>(pendingAdditions)
            additionsList.add(additions)
            pendingAdditions.clear()
            val adder = Runnable {
                additions.forEach(::animateAddImpl)
                additions.clear()
                additionsList.remove(additions)
            }
            if (removalsPending || movesPending || changesPending) {
                val removeDelay = if (removalsPending) removeDuration else 0L
                val moveDelay = if (movesPending) moveDuration else 0L
                val changeDelay = if (changesPending) changeDuration else 0L
                val totalDelay = removeDelay + maxOf(moveDelay, changeDelay)
                ViewCompat.postOnAnimationDelayed(additions.first().itemView, adder, totalDelay)
            } else {
                adder.run()
            }
        }
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        pendingRemovals.add(holder)
        return true
    }

    private fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        removeAnimations.add(holder)
        animation.setDuration(removeDuration).alpha(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) = dispatchRemoveStarting(holder)

            override fun onAnimationEnd(animator: Animator) {
                animation.setListener(null)
                view.alpha = 1f
                dispatchRemoveFinished(holder)
                removeAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        }).start()
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        holder.itemView.alpha = 0f
        pendingAdditions.add(holder)
        return true
    }

    private fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        addAnimations.add(holder)
        animation.alpha(1f).setDuration(addDuration).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) = dispatchAddStarting(holder)

            override fun onAnimationCancel(animator: Animator) {
                view.alpha = 1f
            }

            override fun onAnimationEnd(animator: Animator) {
                animation.setListener(null)
                dispatchAddFinished(holder)
                addAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        }).start()
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
    ): Boolean {
        val view = holder.itemView
        val adjustedFromX = fromX + view.translationX.toInt()
        val adjustedFromY = fromY + view.translationY.toInt()
        resetAnimation(holder)
        val deltaX = toX - adjustedFromX
        val deltaY = toY - adjustedFromY
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder)
            return false
        }
        if (deltaX != 0) view.translationX = -deltaX.toFloat()
        if (deltaY != 0) view.translationY = -deltaY.toFloat()
        pendingMoves.add(MoveInfo(holder, adjustedFromX, adjustedFromY, toX, toY))
        return true
    }

    private fun animateMoveImpl(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
    ) {
        val view = holder.itemView
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        if (deltaX != 0) view.animate().translationX(0f)
        if (deltaY != 0) view.animate().translationY(0f)
        val animation = view.animate()
        moveAnimations.add(holder)
        animation.setDuration(moveDuration).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) = dispatchMoveStarting(holder)

            override fun onAnimationCancel(animator: Animator) {
                if (deltaX != 0) view.translationX = 0f
                if (deltaY != 0) view.translationY = 0f
            }

            override fun onAnimationEnd(animator: Animator) {
                animation.setListener(null)
                dispatchMoveFinished(holder)
                moveAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        }).start()
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder?,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
    ): Boolean {
        if (oldHolder === newHolder) return animateMove(oldHolder, fromX, fromY, toX, toY)
        val oldView = oldHolder.itemView
        val previousTranslationX = oldView.translationX
        val previousTranslationY = oldView.translationY
        val previousAlpha = oldView.alpha
        resetAnimation(oldHolder)
        val deltaX = (toX - fromX - previousTranslationX).toInt()
        val deltaY = (toY - fromY - previousTranslationY).toInt()
        oldView.translationX = previousTranslationX
        oldView.translationY = previousTranslationY
        oldView.alpha = previousAlpha
        if (newHolder != null) {
            resetAnimation(newHolder)
            newHolder.itemView.translationX = -deltaX.toFloat()
            newHolder.itemView.translationY = -deltaY.toFloat()
            newHolder.itemView.alpha = 0f
        }
        pendingChanges.add(ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY))
        return true
    }

    private fun animateChangeImpl(change: ChangeInfo) {
        val oldHolder = change.oldHolder
        val newHolder = change.newHolder
        val oldView = oldHolder?.itemView
        val newView = newHolder?.itemView
        if (oldHolder != null && oldView != null) {
            val oldAnimation = oldView.animate().setDuration(changeDuration)
            changeAnimations.add(oldHolder)
            oldAnimation.translationX((change.toX - change.fromX).toFloat())
            oldAnimation.translationY((change.toY - change.fromY).toFloat())
            oldAnimation.alpha(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) = dispatchChangeStarting(oldHolder, true)

                override fun onAnimationEnd(animator: Animator) {
                    oldAnimation.setListener(null)
                    oldView.alpha = 1f
                    oldView.translationX = 0f
                    oldView.translationY = 0f
                    dispatchChangeFinished(oldHolder, true)
                    changeAnimations.remove(oldHolder)
                    dispatchFinishedWhenDone()
                }
            }).start()
        }
        if (newHolder != null && newView != null) {
            val newAnimation = newView.animate()
            changeAnimations.add(newHolder)
            newAnimation.translationX(0f).translationY(0f).setDuration(changeDuration)
                .alpha(1f).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animator: Animator) = dispatchChangeStarting(newHolder, false)

                    override fun onAnimationEnd(animator: Animator) {
                        newAnimation.setListener(null)
                        newView.alpha = 1f
                        newView.translationX = 0f
                        newView.translationY = 0f
                        dispatchChangeFinished(newHolder, false)
                        changeAnimations.remove(newHolder)
                        dispatchFinishedWhenDone()
                    }
                }).start()
        }
    }

    private fun endChangeAnimation(infoList: MutableList<ChangeInfo>, item: RecyclerView.ViewHolder) {
        for (index in infoList.indices.reversed()) {
            val info = infoList[index]
            if (endChangeAnimationIfNecessary(info, item) && info.oldHolder == null && info.newHolder == null) {
                infoList.removeAt(index)
            }
        }
    }

    private fun endChangeAnimationIfNecessary(change: ChangeInfo) {
        change.oldHolder?.let { endChangeAnimationIfNecessary(change, it) }
        change.newHolder?.let { endChangeAnimationIfNecessary(change, it) }
    }

    private fun endChangeAnimationIfNecessary(
        change: ChangeInfo,
        item: RecyclerView.ViewHolder,
    ): Boolean {
        val oldItem: Boolean
        when {
            change.newHolder === item -> {
                change.newHolder = null
                oldItem = false
            }
            change.oldHolder === item -> {
                change.oldHolder = null
                oldItem = true
            }
            else -> return false
        }
        item.itemView.apply {
            alpha = 1f
            translationX = 0f
            translationY = 0f
        }
        dispatchChangeFinished(item, oldItem)
        return true
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        val view = item.itemView
        view.animate().cancel()

        for (index in pendingMoves.indices.reversed()) {
            if (pendingMoves[index].holder === item) {
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(item)
                pendingMoves.removeAt(index)
            }
        }
        endChangeAnimation(pendingChanges, item)
        if (pendingRemovals.remove(item)) {
            view.alpha = 1f
            dispatchRemoveFinished(item)
        }
        if (pendingAdditions.remove(item)) {
            view.alpha = 1f
            dispatchAddFinished(item)
        }

        for (index in changesList.indices.reversed()) {
            val changes = changesList[index]
            endChangeAnimation(changes, item)
            if (changes.isEmpty()) changesList.removeAt(index)
        }
        for (index in movesList.indices.reversed()) {
            val moves = movesList[index]
            for (moveIndex in moves.indices.reversed()) {
                if (moves[moveIndex].holder === item) {
                    view.translationY = 0f
                    view.translationX = 0f
                    dispatchMoveFinished(item)
                    moves.removeAt(moveIndex)
                    if (moves.isEmpty()) movesList.removeAt(index)
                    break
                }
            }
        }
        for (index in additionsList.indices.reversed()) {
            val additions = additionsList[index]
            if (additions.remove(item)) {
                view.alpha = 1f
                dispatchAddFinished(item)
                if (additions.isEmpty()) additionsList.removeAt(index)
            }
        }

        removeAnimations.remove(item)
        addAnimations.remove(item)
        changeAnimations.remove(item)
        moveAnimations.remove(item)
        dispatchFinishedWhenDone()
    }

    private fun resetAnimation(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().interpolator = defaultInterpolator
        endAnimation(holder)
    }

    override fun isRunning(): Boolean = pendingAdditions.isNotEmpty() ||
        pendingChanges.isNotEmpty() || pendingMoves.isNotEmpty() || pendingRemovals.isNotEmpty() ||
        moveAnimations.isNotEmpty() || removeAnimations.isNotEmpty() || addAnimations.isNotEmpty() ||
        changeAnimations.isNotEmpty() || movesList.isNotEmpty() || additionsList.isNotEmpty() ||
        changesList.isNotEmpty()

    private fun dispatchFinishedWhenDone() {
        if (!isRunning) dispatchAnimationsFinished()
    }

    override fun endAnimations() {
        for (index in pendingMoves.indices.reversed()) {
            val move = pendingMoves[index]
            move.holder.itemView.apply {
                translationY = 0f
                translationX = 0f
            }
            dispatchMoveFinished(move.holder)
            pendingMoves.removeAt(index)
        }
        for (index in pendingRemovals.indices.reversed()) {
            dispatchRemoveFinished(pendingRemovals[index])
            pendingRemovals.removeAt(index)
        }
        for (index in pendingAdditions.indices.reversed()) {
            val holder = pendingAdditions[index]
            holder.itemView.alpha = 1f
            dispatchAddFinished(holder)
            pendingAdditions.removeAt(index)
        }
        pendingChanges.forEach(::endChangeAnimationIfNecessary)
        pendingChanges.clear()
        if (!isRunning) return

        for (listIndex in movesList.indices.reversed()) {
            val moves = movesList[listIndex]
            for (moveIndex in moves.indices.reversed()) {
                val move = moves[moveIndex]
                move.holder.itemView.apply {
                    translationY = 0f
                    translationX = 0f
                }
                dispatchMoveFinished(move.holder)
                moves.removeAt(moveIndex)
                if (moves.isEmpty()) movesList.removeAt(listIndex)
            }
        }
        for (listIndex in additionsList.indices.reversed()) {
            val additions = additionsList[listIndex]
            for (itemIndex in additions.indices.reversed()) {
                val holder = additions[itemIndex]
                holder.itemView.alpha = 1f
                dispatchAddFinished(holder)
                additions.removeAt(itemIndex)
                if (additions.isEmpty()) additionsList.removeAt(listIndex)
            }
        }
        for (listIndex in changesList.indices.reversed()) {
            val changes = changesList[listIndex]
            for (changeIndex in changes.indices.reversed()) {
                endChangeAnimationIfNecessary(changes[changeIndex])
                if (changes.isEmpty()) changesList.removeAt(listIndex)
            }
        }

        cancelAll(removeAnimations)
        cancelAll(moveAnimations)
        cancelAll(addAnimations)
        cancelAll(changeAnimations)
        dispatchAnimationsFinished()
    }

    private fun cancelAll(holders: List<RecyclerView.ViewHolder>) {
        holders.asReversed().forEach { it.itemView.animate().cancel() }
    }

    override fun canReuseUpdatedViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        payloads: List<Any>,
    ): Boolean = payloads.isNotEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads)

    private companion object {
        const val ANIMATION_DURATION = 180L
        val defaultInterpolator: TimeInterpolator = ViewUtils.CUBIC_INTERPOLATOR
    }
}
