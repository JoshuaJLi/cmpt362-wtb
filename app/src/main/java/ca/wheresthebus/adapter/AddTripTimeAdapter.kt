package ca.wheresthebus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AddTripTimeAdapter(
    private val schedules: MutableList<Pair<MutableList<DayOfWeek>, LocalTime>>,
    private val supportFragmentManager: FragmentManager,
    private val rootView: View
) : RecyclerView.Adapter<AddTripTimeAdapter.AddTripTimeViewHolder>() {

    class AddTripTimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val daysButton: Button = view.findViewById(R.id.add_trip_days_button)
        val timeButton: Button = view.findViewById(R.id.add_trip_time_button)
        val deleteButton: Button = view.findViewById(R.id.add_trip_delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddTripTimeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_trip_time, parent, false)
        return AddTripTimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddTripTimeViewHolder, position: Int) {
        val schedule = schedules[position]

        holder.daysButton.text = schedule.first.joinToString(", ") {
            it.name.substring(0, 3)
                .lowercase()
                .replaceFirstChar { char -> char.uppercase() }
        }

        val timeString = schedule.second.format(DateTimeFormatter.ofPattern("h:mm a"))
        holder.timeButton.text = timeString

        holder.daysButton.setOnClickListener { handleDaysButtonClick(holder) }
        holder.timeButton.setOnClickListener { handleTimeButtonClick(holder) }
        holder.deleteButton.setOnClickListener { handleDeleteButtonClick(holder) }
    }

    override fun getItemCount(): Int {
        return schedules.size
    }

    private fun handleDaysButtonClick(holder: AddTripTimeViewHolder) {
        val position = holder.adapterPosition
        val currentDays = schedules[position].first

        val dialogView = LayoutInflater
            .from(holder.itemView.context)
            .inflate(R.layout.dialog_add_trip_time_day, null)

        val checkBoxes = mapOf(
            DayOfWeek.MONDAY to dialogView.findViewById<CheckBox>(R.id.monday_checkbox),
            DayOfWeek.TUESDAY to dialogView.findViewById<CheckBox>(R.id.tuesday_checkbox),
            DayOfWeek.WEDNESDAY to dialogView.findViewById<CheckBox>(R.id.wednesday_checkbox),
            DayOfWeek.THURSDAY to dialogView.findViewById<CheckBox>(R.id.thursday_checkbox),
            DayOfWeek.FRIDAY to dialogView.findViewById<CheckBox>(R.id.friday_checkbox),
            DayOfWeek.SATURDAY to dialogView.findViewById<CheckBox>(R.id.saturday_checkbox),
            DayOfWeek.SUNDAY to dialogView.findViewById<CheckBox>(R.id.sunday_checkbox)
        )

        // Attach listener to update currentDays according to checked boxes
        checkBoxes.forEach { (day, checkBox) ->
            checkBox.isChecked = currentDays.contains(day)
        }

        // Attach listener to enforce at least one checkbox is checked
        checkBoxes.forEach { (_, checkBox) ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked) {
                    val checkedCount = checkBoxes.values.count { it.isChecked }
                    if (checkedCount == 0) {
                        checkBox.isChecked = true
                    }
                }
            }
        }

        val dialog = MaterialAlertDialogBuilder(holder.itemView.context)
            .setTitle("Select Notification days")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selectedDays = checkBoxes.filter { it.value.isChecked }.keys.toMutableList()
                schedules[position] = schedules[position].copy(first = selectedDays)
                notifyItemChanged(position)
            }
            .setNegativeButton("Cancel", null)

        dialog.show()
    }

    private fun handleTimeButtonClick(holder: AddTripTimeViewHolder) {
        val currentHour = schedules[holder.adapterPosition].second.hour
        val currentMinute = schedules[holder.adapterPosition].second.minute

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Select Notification time")
            .build()

        picker.addOnPositiveButtonClickListener {
            val position = holder.adapterPosition
            schedules[position] = schedules[position].copy(second = LocalTime.of(picker.hour, picker.minute))
            notifyItemChanged(position)
        }

        picker.show(supportFragmentManager, "MaterialTimePicker")
    }

    private fun handleDeleteButtonClick(holder: AddTripTimeViewHolder) {
        schedules.removeAt(holder.adapterPosition)
        notifyItemRemoved(holder.adapterPosition)
    }

    fun addTime(schedulePair: Pair<MutableList<DayOfWeek>, LocalTime>) {
        if (schedules.size >= 5) {
            Snackbar.make(rootView, "Maximum number of schedules reached", Snackbar.LENGTH_SHORT).show()
            return
        }
        schedules.add(schedulePair)
        notifyItemInserted(schedules.size - 1)
    }

}