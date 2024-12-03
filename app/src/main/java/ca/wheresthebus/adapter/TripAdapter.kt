package ca.wheresthebus.adapter

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ca.wheresthebus.R
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.utils.TextUtils
import com.google.android.material.button.MaterialButton
import java.time.LocalDateTime

class TripAdapter(
    private val dataSet : ArrayList<ScheduledTrip> = arrayListOf(),
    private val onDeleteSwipe: (ScheduledTrip) -> Unit
) : RecyclerView.Adapter<TripAdapter.ActiveTripViewHolder>() {

    companion object {
        object ViewType {
            const val ACTIVE = 0
            const val TODAY = 1
            const val LATER = 2
        }
    }

    inner class ActiveTripViewHolder(view: View, private val viewType: Int) : ViewHolder(view) {
        private val nickname : TextView = view.findViewById(R.id.text_trip_nickname)
        private val active : TextView = view.findViewById(R.id.text_trip_active_time)
        private var stops : RecyclerView = view.findViewById(R.id.recycler_trips_recycler_stops)
        private var bell : MaterialButton = view.findViewById(R.id.button_trip_do_notifications)
        private lateinit var stopAdapter: FavStopAdapter

        fun bind(trip : ScheduledTrip) {
            val adapterType = when(viewType) {
                (ViewType.ACTIVE) -> FavStopAdapter.Type.TRIP_ACTIVE
                else -> FavStopAdapter.Type.TRIP_INACTIVE
            }

            if (viewType != ViewType.ACTIVE) {
                bell.visibility = View.GONE
            }

            active.text = TextUtils.ScheduledTripText.getActivityStatus(trip)
            nickname.text = trip.nickname
            stopAdapter = FavStopAdapter(trip.stops, adapterType)

            stops.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = stopAdapter
            }
        }
        init {
            view.setOnClickListener {  }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveTripViewHolder {
        LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false).let {
            return ActiveTripViewHolder(it, viewType)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: ActiveTripViewHolder, position: Int) {
        dataSet[position].let {
            holder.bind(it)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val trip = dataSet[position]
        val currentTime = LocalDateTime.now()

        if (trip.isActive(currentTime)) {
            return ViewType.ACTIVE
        } else if (trip.isToday(currentTime)) {
            return ViewType.TODAY
        }

        return ViewType.LATER
    }

    fun updateData(trips : List<ScheduledTrip>) {
        dataSet.clear()
        dataSet.addAll(trips)
        notifyDataSetChanged()
    }

    // Builds and returns an ItemTouchHelper for TripAdapter cards
    // Sets the onSwiped callback to delete a ScheduledTrip object from the dataset
    fun getSwipeHandler(): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                target: ViewHolder
            ): Boolean {
                return false
            }

            // Delete on swipe left of card
            override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT){
                    val position = viewHolder.adapterPosition
                    val tripToDelete = dataSet[position]
                    onDeleteSwipe(tripToDelete)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // only move the foreground
                val itemView = viewHolder.itemView.findViewById<View>(R.id.trip_card_foreground)
                itemView.translationX = dX
            }
        }
    }
}