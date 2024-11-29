package ca.wheresthebus.ui.trips

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ca.wheresthebus.data.model.Schedule
import ca.wheresthebus.databinding.FragmentTripsBinding
import ca.wheresthebus.service.AlarmService
import ca.wheresthebus.service.AlarmService.Companion
import ca.wheresthebus.service.AlarmService.Companion.notificationId

class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(TripsViewModel::class.java)

        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        scheduleTripNotifications(listOf(), requireContext())

        return root
    }

    fun scheduleTripNotifications(trips : List<Schedule>, context: Context) {
        val intent = Intent(context, AlarmService::class.java)
        val title = "title"
        val message = "message"
        intent.putExtra(AlarmService.titleExtra, title)
        intent.putExtra(AlarmService.messageExtra, message)
        context.sendBroadcast(intent)
        val pendingIntent =  PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val time = System.currentTimeMillis() + (1000 * 5)
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}