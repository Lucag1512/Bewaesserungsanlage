package com.example.t3100.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.t3100.R
import com.example.t3100.databinding.FragmentLaunchBinding

class LaunchFragment : Fragment() {

    companion object {
        fun newInstance() = LaunchFragment()
    }

    private lateinit var binding: FragmentLaunchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_launch, container, false)

        binding.btnForwardPlantList.setOnClickListener {
            findNavController().navigate(LaunchFragmentDirections.actionSecondFragmentToPlantListFragment())
        }

        binding.btnForwardManualWatering.setOnClickListener {
            findNavController().navigate(LaunchFragmentDirections.actionLaunchfragmentToBluetoothFragmentManualWatering())
        }

        binding.btnForwardDeleteData.setOnClickListener{
            findNavController().navigate(LaunchFragmentDirections.actionLaunchfragmentToDeleteDataOnMikroncontollerFragment())
        }

        //binding.tvMessage.text = args.data2

        //findNavController().popBackStack()
        return binding.root
    }

}