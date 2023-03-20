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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t3100.App
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.adapter.WateringTimesAdapter
import com.example.t3100.data.Plant
import com.example.t3100.databinding.FragmentEditplantBinding
import com.example.t3100.viewmodel.SharedViewModel
import com.google.gson.Gson


class EditPlantFragment : Fragment(), WateringTimesAdapter.ItemClickListener {

    companion object;

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var adapter: WateringTimesAdapter

    private lateinit var binding: FragmentEditplantBinding

    lateinit var oldPlant: Plant

    private val args: EditPlantFragmentArgs by navArgs()

    var oldCalibrationValue = 13.33

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        //Einfacherer Zugriff auf Objekte des xml Flies
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_editplant, container, false)

        activity?.title = "Pflanze anpassen"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        oldPlant = sharedViewModel.plantList[args.position]

        if (oldPlant.valve == 1) {
            adapter = WateringTimesAdapter(
                sharedViewModel.plantList[args.position].wateringList,
                this,
                ((activity?.application as? App)?.calibrationValue1!!)
            )
            binding.rvWateringTimes.adapter = adapter
            oldCalibrationValue = ((activity?.application as? App)?.calibrationValue1!!)
        } else if (oldPlant.valve == 2) {
            adapter = WateringTimesAdapter(
                sharedViewModel.plantList[args.position].wateringList,
                this,
                ((activity?.application as? App)?.calibrationValue2!!)
            )
            binding.rvWateringTimes.adapter = adapter
            oldCalibrationValue = ((activity?.application as? App)?.calibrationValue2!!)
        } else if (oldPlant.valve == 3) {
            adapter = WateringTimesAdapter(
                sharedViewModel.plantList[args.position].wateringList,
                this,
                ((activity?.application as? App)?.calibrationValue3!!)
            )
            binding.rvWateringTimes.adapter = adapter
            oldCalibrationValue = ((activity?.application as? App)?.calibrationValue3!!)
        }

        //Festlegen der auszuwählenden Ventile
        binding.spinnerValve.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Ventil 1", "Ventil 2", "Ventil 3")
        )

        //Aktuelle Pflanzendaten anzeigen :
        // Name
        binding.etNewPlantName.setText(sharedViewModel.plantList[args.position].name)

        //Ventil
        binding.spinnerValve.setSelection((sharedViewModel.plantList[args.position].valve) - 1)
        //Ende aktuelle Werte übernehmen

        binding.btnAddWateringElement.setOnClickListener {
            findNavController().navigate(
                EditPlantFragmentDirections.actionEditPlantFragmentToEditWateringElementFragment(
                    args.position,
                    null,
                    oldCalibrationValue.toString()
                )
            )
        }

        //Prüfung kann Pflanze editiert werden (Doppelungen/Name leer)
        //Keine Prüfung auf alte Werte
        binding.btnSavePlant.setOnClickListener {

            if ((binding.etNewPlantName.text.isEmpty())) {
                Toast.makeText(
                    requireContext(),
                    "Name der Pflanze darf nicht leer sein",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (oldPlant.valve != (binding.spinnerValve.selectedItemPosition + 1) &&
                sharedViewModel.plantList.any { plant -> plant.valve == (binding.spinnerValve.selectedItemPosition + 1) }
            ) {
                Toast.makeText(requireContext(), "Ventil ist bereits belegt", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            if (oldPlant.name != binding.etNewPlantName.text.toString().trim() &&
                sharedViewModel.plantList.any { plant ->
                    plant.name?.lowercase() == binding.etNewPlantName.text.toString().trim().lowercase()
                }
            ) {

                Toast.makeText(requireContext(), "Name bereits vergeben", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val oldValve = oldPlant.valve

            //Eingegebene Werte vom Nutzer in Pflanzenliste übernehmen und anpassen
            sharedViewModel.plantList[args.position].name =
                binding.etNewPlantName.text.toString().trim()
            sharedViewModel.plantList[args.position].valve =
                binding.spinnerValve.selectedItemPosition + 1

            if (oldValve == (binding.spinnerValve.selectedItemPosition + 1)) {
                savePlant()
            } else {
                var newCalibrationValue = 13.33

                if (sharedViewModel.plantList[args.position].valve == 1) {
                    newCalibrationValue = ((activity?.application as? App)?.calibrationValue1!!)
                } else if (sharedViewModel.plantList[args.position].valve == 2) {
                    newCalibrationValue = ((activity?.application as? App)?.calibrationValue2!!)
                } else if (sharedViewModel.plantList[args.position].valve == 3) {
                    newCalibrationValue = ((activity?.application as? App)?.calibrationValue3!!)
                }

                val sizeWateringElementList =
                    sharedViewModel.plantList[args.position].wateringList.size
                for (i in 0..(sizeWateringElementList - 1)) {
                    sharedViewModel.plantList[args.position].wateringList[i].water =
                        sharedViewModel.plantList[args.position].wateringList[i].water * oldCalibrationValue / newCalibrationValue
                }
                savePlant()
            }
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


    fun savePlant() {

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

    override fun onEditClick(pos: Int) {
        findNavController().navigate(
            EditPlantFragmentDirections.actionEditPlantFragmentToEditWateringElementFragment(
                args.position,
                pos.toString(),
                oldCalibrationValue.toString()
            )
        )
    }

    override fun onDeleteClick(pos: Int) {
        sharedViewModel.plantList[args.position].wateringList.removeAt(pos)
        adapter.notifyDataSetChanged()
    }
}