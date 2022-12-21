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
import androidx.navigation.fragment.navArgs
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.data.Plant
import com.example.t3100.databinding.FragmentEditplantBinding
import com.example.t3100.viewmodel.SharedViewModel
import com.google.gson.Gson
import java.util.*


class EditPlantFragment : Fragment(), TimePickerDialog.OnTimeSetListener {

    companion object {
    }

    private val sharedViewModel : SharedViewModel by activityViewModels()

    private lateinit var binding: FragmentEditplantBinding

    var hour = 12
    var minute = 0

    var savedHour = 12
    var savedMinute = 0

    lateinit var oldPlant : Plant

    private val args :  EditPlantFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.title = "Pflanze anpassen"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        oldPlant = sharedViewModel.plantList[args.position]

        //Einfacherer Zugriff auf Objekte des xml Flies
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_editplant, container, false)

        //Festlegen der auszuwählenden Ventile
        binding.spinnerValve.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, arrayOf("Ventil 1", "Ventil 2", "Ventil 3"))

        //Aktuelle Pflanzendaten anzeigen :
        // Name
        binding.etNewPlantName.setText(sharedViewModel.plantList[args.position].name)

        //Wassermenge
        binding.seekBarWater.setProgress(((sharedViewModel.plantList[args.position].water)*0.018/100).toInt())
        binding.tvWater.text = "Tägliche Wassermenge ${((sharedViewModel.plantList[args.position].water)*0.018).toInt()} mL"

        //Bewässerungszeitpunkt
        savedHour = sharedViewModel.plantList[args.position].hour
        savedMinute = sharedViewModel.plantList[args.position].minute
        if(sharedViewModel.plantList[args.position].hour <10 && sharedViewModel.plantList[args.position].minute <10){
            binding.tvWateringTime.text = "Bewässerungszeitpunkt 0${sharedViewModel.plantList[args.position].hour}:0${sharedViewModel.plantList[args.position].minute}"
        }else if(sharedViewModel.plantList[args.position].hour <10){
            binding.tvWateringTime.text = "Bewässerungszeitpunkt 0${sharedViewModel.plantList[args.position].hour}:${sharedViewModel.plantList[args.position].minute}"
        } else if(sharedViewModel.plantList[args.position].minute <10){
            binding.tvWateringTime.text = "Bewässerungszeitpunkt ${sharedViewModel.plantList[args.position].hour}:0${sharedViewModel.plantList[args.position].minute}"
        } else {
            binding.tvWateringTime.text = "Bewässerungszeitpunkt ${sharedViewModel.plantList[args.position].hour}:${sharedViewModel.plantList[args.position].minute}"
        }

        //Ventil
        binding.spinnerValve.setSelection((sharedViewModel.plantList[args.position].valve)-1)

        //Ende aktuelle Werte übernehmen

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

        //Prüfung kann Pflanze editiert werden (Doppelungen/Name leer)
        //Keine Prüfung auf alte Werte
        binding.btnEditPlant.setOnClickListener {

            if ((binding.etNewPlantName.text.isEmpty())) {
                Toast.makeText(requireContext(), "Name der Pflanze darf nicht leer sein", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(oldPlant.valve != (binding.spinnerValve.selectedItemPosition + 1) &&
                sharedViewModel.plantList.any {plant -> plant.valve == (binding.spinnerValve.selectedItemPosition + 1) }){
                Toast.makeText(requireContext(), "Ventil ist bereits belegt", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(oldPlant.name != binding.etNewPlantName.text.toString().trim() &&
                sharedViewModel.plantList.any {plant -> plant.name == binding.etNewPlantName.text.toString().trim() }) {

                Toast.makeText(requireContext(), "Name bereits vergeben", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            //Eingegebene Werte vom Nutzer in Pflanzenliste übernehmen und anpassen
            sharedViewModel.plantList[args.position].name = binding.etNewPlantName.text.toString().trim()
            sharedViewModel.plantList[args.position].water = (binding.seekBarWater.progress)*100/0.018
            sharedViewModel.plantList[args.position].valve = binding.spinnerValve.selectedItemPosition + 1
            sharedViewModel.plantList[args.position].minute = savedMinute
            sharedViewModel.plantList[args.position].hour = savedHour

            savePlant()

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root

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


    fun savePlant(){

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
}