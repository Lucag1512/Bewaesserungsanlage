package com.example.t3100.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t3100.R
import com.example.t3100.adapter.PlantAdapter
import com.example.t3100.data.Plant
import com.example.t3100.databinding.FragmentPlantlistBinding
import com.example.t3100.viewmodel.SharedViewModel
import com.google.gson.Gson
import java.util.*


class PlantListFragment : Fragment(), PlantAdapter.ItemClickListener {

    companion object {
        fun newInstance() = PlantListFragment()
    }

    private lateinit var binding: FragmentPlantlistBinding

    private val sharedViewModel : SharedViewModel by activityViewModels()

    private lateinit var adapter: PlantAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.title = "Pflanzenliste"

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_plantlist, container, false)

        //Buttons ausblenden

        //TODO: Einblenden wenn wenn Pfalnze gelöscht
        //Daten übetragen ausblenden wenn keine Plfanze angelegt ist
        if(sharedViewModel.plantList.size == 0){
            binding.btnShareData.visibility = View.GONE
        } else{
            binding.btnShareData.visibility = View.VISIBLE
        }

        //Pflanze hinzufügen ausblenden wenn bereits 3 Elemente angelegt wurden
        if(sharedViewModel.plantList.size == 3){
            binding.btnAddPlant.visibility = View.INVISIBLE
        } else{
            binding.btnAddPlant.visibility = View.VISIBLE
        }

        //findNavController().popBackStack()

        getSavedPlants()

        adapter = PlantAdapter(sharedViewModel.plantList, this)
        binding.rvPlants.adapter = adapter
        binding.rvPlants.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            (binding.rvPlants.layoutManager as LinearLayoutManager).getOrientation()
        )
        binding.rvPlants.addItemDecoration(dividerItemDecoration)


        binding.btnAddPlant.setOnClickListener {
           findNavController().navigate(PlantListFragmentDirections.actionPlantListFragmentToAddPlantFragment())
        }

        binding.btnShareData.setOnClickListener {
            findNavController().navigate(PlantListFragmentDirections.actionPlantListFragmentToBluetoothFragment(
                sharedViewModel.plantList.toTypedArray()
            ))
        }

        return binding.root

    }


    fun getSavedPlants(){
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        val plantString = sharedPref?.getString(getString(R.string.plant_list_key), "")?:""

        val gson = Gson()
        val savedPlantList = gson.fromJson(plantString, Array<Plant>::class.java)?: arrayOf()
        sharedViewModel.plantList = savedPlantList.toMutableList()

        if(sharedViewModel.plantList.size == 0){
            binding.btnShareData.visibility = View.GONE
        } else{
            binding.btnShareData.visibility = View.VISIBLE
        }
    }

    override fun onDeleteClick(pos: Int) {

        sharedViewModel.plantList.removeAt(pos)
        adapter.notifyItemRemoved(pos)

        //Save to phone
        val gson = Gson()
        val plantString = gson.toJson(sharedViewModel.plantList)

        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        sharedPref?.let { storage ->
            with (storage.edit()) {
                putString(getString(R.string.plant_list_key), plantString)
                apply()
            }
        }

        if(sharedViewModel.plantList.size == 0){
            binding.btnShareData.visibility = View.GONE
        } else{
            binding.btnShareData.visibility = View.VISIBLE
        }

        //Pflanze hinzufügen ausblenden wenn bereits 3 Elemente angelegt wurden
        if(sharedViewModel.plantList.size == 3){
            binding.btnAddPlant.visibility = View.INVISIBLE
        } else{
            binding.btnAddPlant.visibility = View.VISIBLE
        }
    }


}