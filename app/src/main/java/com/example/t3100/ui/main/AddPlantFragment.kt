package com.example.t3100.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t3100.App
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.adapter.WateringTimesAdapter
import com.example.t3100.data.Plant
import com.example.t3100.databinding.FragmentAddplantBinding
import com.example.t3100.viewmodel.SharedViewModel
import com.google.gson.Gson


class AddPlantFragment : Fragment(), WateringTimesAdapter.ItemClickListener {

    //Zugriff auf das SharedViewModel herstellen
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var adapter: WateringTimesAdapter

    private lateinit var binding: FragmentAddplantBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.title = "Pflanze hinzufügen"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Einfacherer Zugriff auf Objekte des xml Flies
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_addplant, container, false)

        //Festlegen der auszuwählenden Ventile
        binding.spinnerValve.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Ventil 1", "Ventil 2", "Ventil 3")
        )

        //Bewässerungszeitpunkt hinzufügen
        binding.btnAddWateringElement.setOnClickListener {
            findNavController().navigate(
                AddPlantFragmentDirections.actionAddPlantFragmentToEditNewPlantWateringElementFragment(
                    null
                )
            )
        }

        /*Defaultwert für Wassermenge übergeben bei Speichern der Pflanze wird passender
        Wert hinterlegt*/
        adapter = WateringTimesAdapter(
            sharedViewModel.tempWateringElementList,
            this,
            13.33
        )
        binding.rvWateringTimes.adapter = adapter

        //Prüfung kann Pflanze hinzugefügt werden (Doppelungen/Name leer)
        binding.btnAddPlant.setOnClickListener {

            if ((binding.etNewPlantName.text.isEmpty())) {
                Toast.makeText(
                    requireContext(),
                    "Name der Pflanze darf nicht leer sein",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (sharedViewModel.plantList.any { plant -> plant.valve == (binding.spinnerValve.selectedItemPosition + 1) }) {
                Toast.makeText(requireContext(), "Ventil ist bereits belegt", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            if (sharedViewModel.plantList.any { plant ->
                    plant.name?.lowercase() == binding.etNewPlantName.text.toString().trim().lowercase()
                }) {
                Toast.makeText(requireContext(), "Name bereits vergeben", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            //Eingegebene Werte vom Nutzer in Variablen übernehmen und anpassen
            val title = binding.etNewPlantName.text.toString().trim()
            val valve = binding.spinnerValve.selectedItemPosition + 1

            //Wassermenge an Ventil anpassen je nach Kalibrierungswert
            val sizeWateringElementList = sharedViewModel.tempWateringElementList.size
            var calibrationValue = 13.33
            if (valve == 1) {
                calibrationValue = ((activity?.application as? App)?.calibrationValue1!!)
            } else if (valve == 2) {
                calibrationValue = ((activity?.application as? App)?.calibrationValue2!!)
            } else if (valve == 3) {
                calibrationValue = ((activity?.application as? App)?.calibrationValue3!!)
            }
            for (i in 0..(sizeWateringElementList - 1)) {
                sharedViewModel.tempWateringElementList[i].water =
                    sharedViewModel.tempWateringElementList[i].water * 13.33 / calibrationValue
            }

            //Variablen in die Datenklasse Plant einfügen und Pflanze speichern
            val plant = Plant(title, valve, sharedViewModel.tempWateringElementList)
            addAndSavePlant(plant)

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        //Trennlinie zwischen den angezeigten Pflanzen einfügen
        binding.rvWateringTimes.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            (binding.rvWateringTimes.layoutManager as LinearLayoutManager).orientation
        )
        binding.rvWateringTimes.addItemDecoration(dividerItemDecoration)


        return binding.root

    }

    //Hinzufügen der Pflanze zur Liste und Liste auf dem Handy speichern
    fun addAndSavePlant(plant: Plant) {

        //Pflanze zur Liste der Pflanzen hinzüfgen
        sharedViewModel.plantList.add(plant)

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
        sharedViewModel.tempWateringElementList.clear()

    }

    override fun onEditClick(pos: Int) {
        findNavController().navigate(
            AddPlantFragmentDirections.actionAddPlantFragmentToEditNewPlantWateringElementFragment(
                pos.toString()
            )
        )
    }

    override fun onDeleteClick(pos: Int) {
        sharedViewModel.tempWateringElementList.removeAt(pos)
        adapter.notifyDataSetChanged()
    }

}