package cz.brno.mendelu.meetup.adapters

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.dataclasses.User
import kotlinx.android.synthetic.main.item_user.view.*

class UsersAdapter(var usersList: MutableList<User>, var isSelectable: Boolean) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {
    public val selectable = isSelectable
    public val selected: MutableList<Int> = ArrayList<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UsersViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val currentItem = usersList[position]
        holder.name.text = currentItem.name
        holder.uid = currentItem.uid
        if (selectable) {
            if (selected.contains(position)) {
                holder.itemView.picture.setImageDrawable(drawableCheckMark())
                return
            }
        }
        holder.picture.setImageDrawable(drawableFirstLetter(currentItem.name!!))
    }

    override fun getItemCount(): Int {
        return usersList.count()
    }

    inner class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val picture: ImageView = itemView.picture
        val name: TextView = itemView.name
        var uid: String? = null
        init {
            if (selectable){
                itemView.setOnClickListener {
                    val pos = adapterPosition
                    if (selected.contains(pos)){
                        selected.remove(pos)
                    }

                    else {
                        itemView.picture.setImageDrawable(drawableCheckMark())
                        selected.add(pos)
                    }
                    notifyItemChanged(pos)
                }
            }
        }
    }

    private fun drawableCheckMark(): Drawable {
        return TextDrawable.builder()
            .buildRound("✔", Color.parseColor("#4caf50"))
    }

    private fun drawableFirstLetter(name: String): Drawable {
        val firstLetter: Char = name[0]
        val generator: ColorGenerator = ColorGenerator.MATERIAL
        return TextDrawable.builder()
            .buildRound(firstLetter.toString(), generator.getColor(name))
    }

}
