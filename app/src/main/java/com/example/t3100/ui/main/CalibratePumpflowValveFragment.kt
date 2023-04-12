package com.example.t3100.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.t3100.App
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.databinding.FragmentCalibratepumpflowvalveBinding
import com.example.t3100.viewmodel.BluetoothViewModel
import com.example.t3100.viewmodel.SharedViewModel
import com.google.gson.Gson

class CalibratePumpflowValveFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var viewModel: BluetoothViewModel

    private lateinit var binding: FragmentCalibratepumpflowvalveBinding

    private val args: CalibratePumpflowValveFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(BluetoothViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.title = "Wassermenge kalibrieren"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Einfacherer Zugriff auf Objekte des xml Flies
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_calibratepumpflowvalve,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var oldCalibrationValue = 0.0
        var newCalibrationValue = 0.0

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        /*Prüfung liegt eingegebener Wert innerhalb logischer Grenzen
        Anschließend Kalibrierungswert des ausgewählten Ventils updaten
         */
        binding.btnSaveValue.setOnClickListener {
            if ((binding.etWaterAmount.text.isEmpty()) || binding.etWaterAmount.text.toString().toInt() <175
                || binding.etWaterAmount.text.toString().toInt() >900) {
                Toast.makeText(
                    requireContext(),
                    "Biite gültige Wassermenge eingeben",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            } else {

                if (args.valve == 1){
                    oldCalibrationValue = ((activity?.application as? App)?.calibrationValue1!!)
                    newCalibrationValue = (binding.etWaterAmount.text.toString()
                        .toDouble()) / 10000
                    //Wert global speichern
                    (activity?.application as? App)?.setNewCalibrationValue1(newCalibrationValue)
                }
                else if(args.valve == 2){
                    oldCalibrationValue = ((activity?.application as? App)?.calibrationValue2!!)
                    newCalibrationValue = (binding.etWaterAmount.text.toString()
                        .toDouble()) / 10000
                    (activity?.application as? App)?.setNewCalibrationValue2(newCalibrationValue)
                }
                else if(args.valve == 3){
                    oldCalibrationValue = ((activity?.application as? App)?.calibrationValue3!!)
                    newCalibrationValue = (binding.etWaterAmount.text.toString()
                        .toDouble()) / 10000
                    (activity?.application as? App)?.setNewCalibrationValue3(newCalibrationValue)
                }

                //Pflanzenliste auf neuen Kalibrieungswert updaten
                val sizePlantList = sharedViewModel.plantList.size
                if (sizePlantList == 0) {
                    findNavController().popBackStack()
                } else {
                    for (i in 0..(sizePlantList - 1)) {
                        //Prüfung ist Pflanze am neu kalibriertem Ventil
                        if (sharedViewModel.plantList[i].valve == args.valve) {
                            val sizeWateringElements: Int =
                                sharedViewModel.plantList[i].wateringList.size
                            if (sizeWateringElements != 0) {
                                for (x in 0..(sizeWateringElements - 1)) {
                                    sharedViewModel.plantList[i].wateringList[x].water =
                                        sharedViewModel.plantList[i].wateringList[x].water * oldCalibrationValue / newCalibrationValue
                                }
                            }
                        }
                    }
                    savePlantList()
                    findNavController().popBackStack(R.id.launchfragment, false)
                }
            }
        }
    }


    //Liste auf dem Handy speichern
    fun savePlantList() {

        //Liste der Pflanzen auf dem Handy speichern
        val gson = Gson()
        val plantString = gson.toJson(sharedViewModel.plantList)
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        sharedPref?.let { storage ->
            with(storage.edit()) {
                putString(getString(R.string.plant_list_key), plantString)
                apply()
            }
        }
    }
}