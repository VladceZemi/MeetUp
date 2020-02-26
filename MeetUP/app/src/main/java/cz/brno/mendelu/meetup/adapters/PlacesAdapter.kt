package cz.brno.mendelu.meetup.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.dataclasses.PlaceVotes
import kotlinx.android.synthetic.main.item_event_votes.view.*


class PlacesAdapter(private val placesList: MutableList<PlaceVotes>, var countOfUsers: Int) :
    RecyclerView.Adapter<PlacesAdapter.PlacesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacesViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_event_votes, parent, false)
        return PlacesViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return placesList.count()
    }

    override fun onBindViewHolder(holder: PlacesViewHolder, position: Int) {
        val currentItem = placesList[position]

        holder.name.text = currentItem.name
        holder.placeId = currentItem.placeId

        val voteText = "${(currentItem.votesCount?.toDouble()?.div(countOfUsers))?.times(other = 100)?.toInt().toString()}%"
        holder.votesText?.text = voteText
    }

    class PlacesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.name
        var placeId: String? = null
        var votesText : TextView? = itemView.votes
        var votesCount : Int? = 0
        }
}
