package com.example.t3100.ui.main

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TimePicker
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.data.Plant
import com.example.t3100.databinding.FragmentAddplantBinding
import com.example.t3100.viewmodel.SharedViewModel
import com.google.gson.Gson
import java.util.*


class AddPlantFragment : Fragment(), TimePickerDialog.OnTimeSetListener {

    //Zugriff auf das SharedViewModel herstellen
    private val sharedViewModel : SharedViewModel by activityViewModels()

    private lateinit var binding: FragmentAddplantBinding

    var hour = 12
    var minute = 0

    var savedHour = 12
    var savedMinute = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.title = "Pflanze hinzufügen"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Einfacherer Zugriff auf Objekte des xml Flies
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_addplant, container, false)

        //Festlegen der auszuwählenden Ventile
        binding.spinnerValve.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, arrayOf("Ventil 1", "Ventil 2", "Ventil 3"))

        //Text in der Benutzeroberfläche an eingestellten Wert anpassen Wert von 1-20 wird mit 100 multipliziert damit 100-2000mL verfügbar sind
        binding.seekBarWater.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                binding.tvWater.text = "Tägliche Wassermenge ${p1*100} mL"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })

        //Gewählte Bewässerungszeit in Variable übernehmen
        binding.btnSelectTime.setOnClickListener {
            getTimeCalender()
            TimePickerDialog(requireContext(), this ,hour,minute,true).show()
        }

        //Prüfung kann Pflanze hinzugefügt werden (Doppelungen/Name leer)
        binding.btnAddPlant.setOnClickListener {

            if ((binding.etNewPlantName.text.isEmpty())) {
                Toast.makeText(requireContext(), "Name der Pflanze darf nicht leer sein", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(sharedViewModel.plantList.any {plant -> plant.valve == (binding.spinnerValve.selectedItemPosition + 1) }){
                Toast.makeText(requireContext(), "Ventil ist bereits belegt", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(sharedViewModel.plantList.any {plant -> plant.name == binding.etNewPlantName.text.toString().trim() }) {
                Toast.makeText(requireContext(), "Name bereits vergeben", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            //Eingegebene Werte vom Nutzer in Variablen übernehmen und anpassen
            val title = binding.etNewPlantName.text.toString().trim()
            val water : Double = (binding.seekBarWater.progress)*100/0.013
            val valve = binding.spinnerValve.selectedItemPosition + 1

            //Variablen in die Datenklasse Plant einfügen und Pflanze speichern
            val plant = Plant(title, water, valve, savedHour, savedMinute)
            addAndSavePlant(plant)

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root

    }

    //Hinzufügen der Pflanze zur Liste und Liste auf dem Handy speichern
    fun addAndSavePlant(plant: Plant){

        //Pflanze zur Liste der Pflanzen hinzüfgen
        sharedViewModel.plantList.add(plant)

        //Liste der Pflanzen auf dem Handy speichern
        val gson = Gson()
        val plantString = gson.toJson(sharedViewModel.plantList)
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        sharedPref?.let { storage ->
            with (storage.edit()) {
                putString(getString(R.string.plant_list_key), plantString)
                apply()
            }
        }

    }

    //Eingestellter Bewässerungszeitpunkt in Textfeld der Benutzeroberfläche übernehemn
    override fun onTimeSet(p0: TimePicker?, hourOfDay: Int, minute: Int) {
        savedHour = hourOfDay
        savedMinute = minute

        if(savedHour <10 && savedMinute <10){
            binding.tvWateringTime.text = "Bewässerungszeitpunkt 0${savedHour}:0${savedMinute}"
        }else if(savedHour <10){
            binding.tvWateringTime.text = "Bewässerungszeitpunkt 0${savedHour}:${savedMinute}"
        } else if(savedMinute <10){
            binding.tvWateringTime.text = "Bewässerungszeitpunkt ${savedHour}:0${savedMinute}"
        } else {
            binding.tvWateringTime.text = "Bewässerungszeitpunkt ${savedHour}:${savedMinute}"
        }
    }

    private fun getTimeCalender(){
        val calendar = Calendar.getInstance()
        hour = calendar.get(Calendar.HOUR_OF_DAY)
        minute = calendar.get(Calendar.MINUTE)
    }
}