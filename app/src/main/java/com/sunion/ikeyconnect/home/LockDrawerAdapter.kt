package com.sunion.ikeyconnect.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.usecase.home.UserSyncOrder
import timber.log.Timber
import java.util.*


class LockDrawerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var listOrder = mutableListOf<UserSyncOrder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyDrawerViewHolder {
        return MyDrawerViewHolder(parent.inflate(R.layout.drawer_list_item))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val order = listOrder[position]
        Timber.d("bind item: $order")
        with(holder.itemView) {
            this.findViewById<TextView>(R.id.tv_lock_description).text = order.DisplayName
            this.findViewById<TextView>(R.id.tv_lock_display_name).text = order.DisplayName
        }
        holder.itemView.onClick {}
    }

    override fun getItemCount(): Int {
        return listOrder.size
    }

    fun moveItem(from: Int, to: Int) {
        Collections.swap(listOrder, from, to)
    }

    fun setData(newDevices: List<UserSyncOrder>) {
        listOrder.clear()
        listOrder.addAll(newDevices)
        notifyDataSetChanged()
    }

    fun getData(): List<UserSyncOrder> {
        return listOrder.toList()
    }

    inner class MyDrawerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

inline fun View.onClick(crossinline clickAction: () -> Unit) = this.setOnClickListener {
    clickAction.invoke()
}

fun ViewGroup.inflate(@LayoutRes layout: Int, attachToBoolean: Boolean = false): View =
    LayoutInflater.from(this.context).inflate(layout, this, attachToBoolean)