package com.sunion.ikeyconnect.home

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.usecase.home.UserSyncOrder
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import kotlinx.coroutines.launch

@Composable
fun DrawerView(lockOrder: MutableList<UserSyncOrder>, onListReOrder: (MutableList<UserSyncOrder>)-> Unit) {
    val myList = remember { mutableStateListOf<UserSyncOrder>() }
    val adapter = remember { LockDrawerAdapter() }
    val scope = rememberCoroutineScope()
    myList.addAll(lockOrder)

    val textStyle = TextStyle(
        color = colorResource(id = R.color.primary),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_30)))
    Text(text = stringResource(id = R.string.menu_title), style = textStyle, modifier = Modifier.padding(20.dp))
    IKeyDivider()
    Column(modifier = Modifier.fillMaxWidth()) {
        val itemTouchHelper by lazy {
            // 1. Note that I am specifying all 4 directions.
            //    Specifying START and END also allows
            //    more organic dragging than just specifying UP and DOWN.
            val simpleItemTouchCallback =
                object : ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or
                            ItemTouchHelper.DOWN or
                            ItemTouchHelper.START or
                            ItemTouchHelper.END,
                    0
                ) {

                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {

                        val adapter = recyclerView.adapter as LockDrawerAdapter
                        val from = viewHolder.bindingAdapterPosition
                        val to = target.bindingAdapterPosition
                        // 2. Update the backing model. Custom implementation in
                        //    MainRecyclerViewAdapter. You need to implement
                        //    reordering of the backing model inside the method.
                        adapter.moveItem(from, to)
                        adapter.notifyItemMoved(from, to)
                        onListReOrder.invoke(adapter.getData() as MutableList)
                        return true
                    }
                    override fun onSwiped(
                        viewHolder: RecyclerView.ViewHolder,
                        direction: Int
                    ) {
                        // 4. Code block for horizontal swipe.
                        //    ItemTouchHelper handles horizontal swipe as well, but
                        //    it is not relevant with reordering. Ignoring here.
                    }
                }
            ItemTouchHelper(simpleItemTouchCallback)
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), //填滿可用空間
            factory = { context ->
                RecyclerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    layoutManager = LinearLayoutManager(context)

                    itemTouchHelper.attachToRecyclerView(this)
                }
            }
        ){ recyclerView ->
            scope.launch {
                recyclerView.adapter = adapter
                adapter.setData(lockOrder)
            }
        }

        IKeyDivider()
        Row(horizontalArrangement = Arrangement.Start,
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_faq),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.space_36))
                    .padding(4.dp)
                    .clickable { }
            )
            Text(text = stringResource(id = R.string.toolbar_title_support), style = textStyle, modifier = Modifier.padding(start = 20.dp))
        }
    }
}