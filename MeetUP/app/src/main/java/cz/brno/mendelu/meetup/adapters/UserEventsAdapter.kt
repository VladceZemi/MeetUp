package cz.brno.mendelu.meetup.adapters


import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.activities.EventDetailActivity
import cz.brno.mendelu.meetup.dataclasses.UserEvent
import kotlinx.android.synthetic.main.item_event.view.*

import kotlinx.android.synthetic.main.item_user.view.name

class UserEventsAdapter(private val eventsList: SortedList<UserEvent>) :
    RecyclerView.Adapter<UserEventsAdapter.UserEventsViewHolder>() {

    private val TAG: String = "UserEventsAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserEventsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return UserEventsViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return eventsList.size()
    }

    override fun onBindViewHolder(holder: UserEventsViewHolder, position: Int) {
        val currentItem = eventsList[position]

        holder.name.text = currentItem.eventName
        holder.eid = currentItem.eid
        val dateTimeFormated = currentItem.eventDate.toString().replace("-",".")
        holder.datetime.text = dateTimeFormated
    }

    class UserEventsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.name
        var eid: String? = null
        val datetime: TextView = itemView.datetime
        init {
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, EventDetailActivity::class.java)
                intent.putExtra("eid", eid)
                itemView.context.startActivity(intent)
            }
        }
    }
}