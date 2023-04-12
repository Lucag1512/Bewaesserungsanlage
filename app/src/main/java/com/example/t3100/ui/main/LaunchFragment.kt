package com.example.t3100.ui.main

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.data.Plant
import com.example.t3100.databinding.FragmentLaunchBinding
import com.example.t3100.viewmodel.SharedViewModel
import com.google.gson.Gson

class LaunchFragment : Fragment() {

    companion object {

        //Deklaration welche Berechtigungen bei welcher SDK benötigt werden
        val REQUIRED_PERMISSIONS_LAUNCH =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var binding: FragmentLaunchBinding

    private var bluetoothAdapter: BluetoothAdapter? = null

    //Berechtigungen für Appfunktionalität anfordern
    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value
            }
            //Sind alle Permissions gegeben wird setupBT gestartet
            if (granted) {
                setupBluetooth()
            } else {
                //Lenht der Nutzer die Berechtigungen ab wird Endlosschleife gestartet
                //Nutzer muss Berechtigungen geben, sonst kann App nicht verwendet werden
                AlertDialog.Builder(requireContext()).create().apply {
                    setTitle("Information")
                    setMessage("Berechtigungen benötigt")
                    setButton(AlertDialog.BUTTON_NEUTRAL, "Erneut freigeben",
                        object : DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                p0?.dismiss()
                                launchPermissionCheck()
                            }
                        })
                    setCancelable(false)
                    show()
                }
            }
        }

    //Prüfung ist BT eingeschaltet worden
    private val bluetoothRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            //Lenht der Nutzer das Einschalten von BT ab wird Endlosschleife gestartet
            //Nutzer muss BT einschalten, sonst kann App nicht verwendet werden
            if (result.resultCode != Activity.RESULT_OK) {
                AlertDialog.Builder(requireContext()).create().apply {
                    setTitle("Information")
                    setMessage("Bluetooth benötigt")
                    setButton(AlertDialog.BUTTON_NEUTRAL, "Erneut freigeben",
                        object : DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                p0?.dismiss()
                                launchBTCheck()
                            }
                        })
                    setCancelable(false)
                    show()
                }
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.title = "Smart Watering"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        //Einfacherer Zugriff auf Objekte des xml Flies
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_launch, container, false)

        launchPermissionCheck()

        //Gespeichterten Pflanzen vom Handy laden
        getSavedPlants()

        //Navigieren zu den weiteren Untermenüs
        binding.btnForwardPlantList.setOnClickListener {
            findNavController().navigate(LaunchFragmentDirections.actionSecondFragmentToPlantListFragment())
        }

        binding.btnForwardManualWatering.setOnClickListener {
            findNavController().navigate(LaunchFragmentDirections.actionLaunchfragmentToManualWateringFragment())
        }

        binding.btnForwardDeleteData.setOnClickListener {
            findNavController().navigate(LaunchFragmentDirections.actionLaunchfragmentToDeleteDataOnMikroncontollerFragment())
        }

        binding.btnForwardCalibrateFlow.setOnClickListener {
            findNavController().navigate(LaunchFragmentDirections.actionLaunchfragmentToCalibratePumpflowFragment())
        }

        return binding.root
    }

    private fun setupBluetooth() {
        //Adapter anlegen, entspricht BT Adapter des lokalen Gerätes
        val bluetoothManager: BluetoothManager? =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(
                requireContext(),
                "Ihr Gerät unterstützt kein Bluetooth",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        launchBTCheck()
    }

    //Prüfung sind alle Permissions gegeben
    private fun checkPermissionsGranted() = REQUIRED_PERMISSIONS_LAUNCH.all { permission ->
        ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun launchPermissionCheck() {
        if (checkPermissionsGranted()) {
            setupBluetooth()
        } else {
            permissionRequest.launch(REQUIRED_PERMISSIONS_LAUNCH)
        }
    }

    //Prüfung ist BT auf dem Gerät eingeschaltet, wenn nicht über Intent anfordern
    private fun launchBTCheck() {
        if (bluetoothAdapter?.isEnabled != true) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothRequest.launch(enableBtIntent)
        }
    }

    //Gespeicherten Daten vom Handy laden
    fun getSavedPlants() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        val plantString = sharedPref?.getString(getString(R.string.plant_list_key), "") ?: ""

        val gson = Gson()
        val savedPlantList = gson.fromJson(plantString, Array<Plant>::class.java) ?: arrayOf()
        sharedViewModel.plantList = savedPlantList.toMutableList()
    }
}


