package com.example.t3100.ui.main

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TimePicker
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.t3100.R
import com.example.t3100.data.WateringElement
import com.example.t3100.databinding.FragmentEditnewplantwateringelementBinding
import com.example.t3100.viewmodel.SharedViewModel
import java.util.*

class EditNewPlantWateringElementFragment : Fragment(), TimePickerDialog.OnTimeSetListener {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var binding: FragmentEditnewplantwateringelementBinding

    private val args: EditNewPlantWateringElementFragmentArgs by navArgs()

    var hour = 12
    var minute = 0

    var savedHour = 12
    var savedMinute = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //Einfacherer Zugriff auf Objekte des xml Flies
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_editnewplantwateringelement,
            container,
            false
        )

        //Werte übernehmen falls bestehender Bewässerungszeitpunkt angepasst wird
        args.wateringelementposition?.toIntOrNull()?.let { wateringElementPosition ->

            val currentWateringElement =
                sharedViewModel.tempWateringElementList[wateringElementPosition]

            //Bewässerungszeitpunkt
            savedHour = currentWateringElement.hour
            savedMinute = currentWateringElement.minute
            if (currentWateringElement.hour < 10 && currentWateringElement.minute < 10) {
                binding.tvWateringTime.text =
                    "Bewässerungszeitpunkt 0${currentWateringElement.hour}:0${currentWateringElement.minute}"
            } else if (currentWateringElement.hour < 10) {
                binding.tvWateringTime.text =
                    "Bewässerungszeitpunkt 0${currentWateringElement.hour}:${currentWateringElement.minute}"
            } else if (currentWateringElement.minute < 10) {
                binding.tvWateringTime.text =
                    "Bewässerungszeitpunkt ${currentWateringElement.hour}:0${currentWateringElement.minute}"
            } else {
                binding.tvWateringTime.text =
                    "Bewässerungszeitpunkt ${currentWateringElement.hour}:${currentWateringElement.minute}"
            }

            //Wassermenge
            binding.seekBarWater.progress =
                ((currentWateringElement.water) / 100 * 13.33).toInt()
            binding.tvWater.text =
                "Tägliche Wassermenge ${((currentWateringElement.water) * 13.33).toInt()} mL"

        }

        //Text in der Benutzeroberfläche an eingestellten Wert anpassen Wert von 1-20 wird mit 100 multipliziert damit 100-2000mL verfügbar sind
        binding.seekBarWater.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                binding.tvWater.text = "Tägliche Wassermenge ${p1 * 100} mL"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })

        binding.btnSave.setOnClickListener {
            /*Sollte der übergebene Wertt null sein handelt es sich um ein neuen
            Bewässerungszeipunkt. Andernfalls wird der alte geupdatet
             */
            if (args.wateringelementposition == null) {
                sharedViewModel.tempWateringElementList.add(
                    WateringElement(
                        (binding.seekBarWater.progress * 100 / 13.33), savedHour, savedMinute
                    )
                )

            } else {
                sharedViewModel.tempWateringElementList[args.wateringelementposition!!.toInt()].water =
                    (binding.seekBarWater.progress * 100 / 13.33)
                sharedViewModel.tempWateringElementList[args.wateringelementposition!!.toInt()].hour =
                    savedHour
                sharedViewModel.tempWateringElementList[args.wateringelementposition!!.toInt()].minute =
                    savedMinute
            }

            findNavController().popBackStack()
        }

        //Gewählte Bewässerungszeit in Variable übernehmen
        binding.btnSelectTime.setOnClickListener {
            getTimeCalender()
            TimePickerDialog(requireContext(), this, hour, minute, true).show()
        }

        return binding.root
    }

    //Eingestellter Bewässerungszeitpunkt in Textfeld der Benutzeroberfläche übernehemn
    override fun onTimeSet(p0: TimePicker?, hourOfDay: Int, minute: Int) {
        savedHour = hourOfDay
        savedMinute = minute

        if (savedHour < 10 && savedMinute < 10) {
            binding.tvWateringTime.text = "Bewässerungszeitpunkt 0${savedHour}:0${savedMinute}"
        } else if (savedHour < 10) {
            binding.tvWateringTime.text = "Bewässerungszeitpunkt 0${savedHour}:${savedMinute}"
        } else if (savedMinute < 10) {
            binding.tvWateringTime.text = "Bewässerungszeitpunkt ${savedHour}:0${savedMinute}"
        } else {
            binding.tvWateringTime.text = "Bewässerungszeitpunkt ${savedHour}:${savedMinute}"
        }
    }

    private fun getTimeCalender() {
        val calendar = Calendar.getInstance()
        hour = calendar.get(Calendar.HOUR_OF_DAY)
        minute = calendar.get(Calendar.MINUTE)
    }

}