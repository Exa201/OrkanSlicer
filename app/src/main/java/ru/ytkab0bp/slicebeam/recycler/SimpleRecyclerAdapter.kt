package ru.ytkab0bp.slicebeam.recycler

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/** Recycler adapter for the app's lightweight, view-backed menu rows. */
open class SimpleRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val viewTypes = HashMap<Class<*>, Int>()
    private val viewCreators = HashMap<Int, SimpleRecyclerItem<*>>()
    private var nextViewType = 0
    private var items: List<SimpleRecyclerItem<*>> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val creator = checkNotNull(viewCreators[viewType]) { "No row factory for view type $viewType" }
        return object : RecyclerView.ViewHolder(creator.onCreateView(parent.context)) {}
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (items[position] as SimpleRecyclerItem<View>).onBindView(holder.itemView)
    }

    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

    @SuppressLint("NotifyDataSetChanged")
    @Suppress("UNCHECKED_CAST")
    open fun setItems(items: List<*>) {
        this.items = items as List<SimpleRecyclerItem<*>>
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        val itemClass = item.javaClass
        val existingType = viewTypes[itemClass]
        if (existingType != null) return existingType

        val type = nextViewType++
        viewTypes[itemClass] = type
        viewCreators[type] = item
        return type
    }

    override fun getItemCount(): Int = items.size

    open fun getItems(): List<SimpleRecyclerItem<*>> = items
}
