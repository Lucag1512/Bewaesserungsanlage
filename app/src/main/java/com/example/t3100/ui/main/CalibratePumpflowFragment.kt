package com.example.t3100.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t3100.App
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.adapter.BluetoothDevicesAdapter
import com.example.t3100.data.ParsedCalibrate
import com.example.t3100.databinding.FragmentCalibratepumpflowBinding
import com.example.t3100.viewmodel.BluetoothViewModel
import com.example.t3100.viewmodel.SharedViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class CalibratePumpflowFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var viewModel: BluetoothViewModel

    private lateinit var binding: FragmentCalibratepumpflowBinding

    private var bluetoothAdapter: BluetoothAdapter? = null

    private lateinit var adapter: BluetoothDevicesAdapter

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
            R.layout.fragment_calibratepumpflow,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AlertDialog.Builder(requireContext()).create().apply {
            setTitle("Hinweis")
            setMessage(
                "Nach Auswahl des entsprechenden Mikrocontrollers wird jedes Ventil für 10s " +
                        "angesteuert. Anschließend bitte gesamt geflossene Menge Wasser eintragen"
            )
            setButton(AlertDialog.BUTTON_POSITIVE, "OK") { dialog, p1 ->
                dialog.dismiss()
            }
            show()
        }


        //Adapter anlegen, entspricht BT Adapter des lokalen Gerätes
        val bluetoothManager: BluetoothManager? =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        //Sendevorgang bei Klick auf gewünschtes BT-Gerät starten
        adapter = BluetoothDevicesAdapter(viewModel.bluetoothDevices, object :
            BluetoothDevicesAdapter.ItemClickListener {
            override fun onItemClick(device: BluetoothDevice) {
                stopSearchDevices()

                binding.textView.visibility = View.INVISIBLE
                binding.rvBluetoothDevices.visibility = View.INVISIBLE
                binding.btnEndSearch.visibility = View.INVISIBLE
                binding.btnStartSearch.visibility = View.INVISIBLE

                //Variable zum JSON Format konvertieren
                val calibrate = ParsedCalibrate(true)
                val gson = Gson()
                val calibrateJson = gson.toJson(calibrate)
                ConnectThread(device).connectAndSend(calibrateJson)

                binding.etWaterAmount.visibility = View.VISIBLE
                binding.btnSaveValue.visibility = View.VISIBLE
                binding.tvWaterHint.visibility = View.VISIBLE


            }
        })

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
                val oldCalibrationValue = ((activity?.application as? App)?.calibrationValue!!)
                val newCalibrationValue = (binding.etWaterAmount.text.toString()
                    .toDouble()) / 3 / 10000
                (activity?.application as? App)?.setNewCalibrationValue(newCalibrationValue)

                //Pflanzenliste auf neuen Kalibrieungswert updaten
                val sizePlantList = sharedViewModel.plantList.size
                if (sizePlantList == 0) {
                    findNavController().popBackStack()
                } else {
                    for (i in 0..(sizePlantList - 1)) {
                        val sizeWateringElements: Int =
                            sharedViewModel.plantList[i].wateringList.size
                        if (sizeWateringElements != 0) {
                            for (x in 0..(sizeWateringElements - 1)) {
                                sharedViewModel.plantList[i].wateringList[x].water =
                                    sharedViewModel.plantList[i].wateringList[x].water * oldCalibrationValue / ((activity?.application as? App)?.calibrationValue!!)
                            }
                        }
                    }
                    savePlantList()
                    findNavController().popBackStack()
                }
            }
        }

        binding.rvBluetoothDevices.adapter = adapter

        //Trennlinie zwischen den angezeigten BT-Geräten einfügen
        binding.rvBluetoothDevices.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            (binding.rvBluetoothDevices.layoutManager as LinearLayoutManager).getOrientation()
        )
        binding.rvBluetoothDevices.addItemDecoration(dividerItemDecoration)

        binding.btnStartSearch.setOnClickListener {
            startSearchDevices()
        }

        binding.btnEndSearch.setOnClickListener {
            stopSearchDevices()
        }

    }

    //Registrieren eines Broadcasts wenn ein Gerät gefunden wurde
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(receiver, filter)
    }

    //BT reciever deaktivieren bei Schließen der App
    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(receiver)
    }

    @SuppressLint("MissingPermission")
    private fun startSearchDevices() {
        binding.btnStartSearch.visibility = View.INVISIBLE
        binding.loadingBarSearch.visibility = View.VISIBLE
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun stopSearchDevices() {
        binding.btnStartSearch.visibility = View.VISIBLE
        binding.loadingBarSearch.visibility = View.GONE
        bluetoothAdapter?.cancelDiscovery()
    }

    //Broadcastreciever erstellen wenn ein Gerät gefunden wurde
    //Gefundenes Gerät in die angezeigte Liste übernehmen
    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {

                    // Gerät wurden gefunden. Info des Gerätes über Intent anfordern
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    if (device?.name?.contains("ESP") == true && !viewModel.bluetoothDevices.contains(
                            device
                        )
                    ) {
                        viewModel.bluetoothDevices.add(device)
                        adapter.notifyItemInserted(viewModel.bluetoothDevices.size - 1)

                    }

                }
            }
        }
    }

    //Verbindung zu einem BT Gerät aufbauen und Klasse zum Daten senden aufrufen
    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {


        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            //UUID frei wählbar (lediglich valide UUID)
            device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
        }

        fun connectAndSend(message: String) {

            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                //Mit dem BT-Gerät über den Socket verbinden. Aufruf blockiert Programmausführung
                //bis Verbindung hergestellt wurde oder ein Fehler erzeugt wird
                try {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        socket.connect()
                        ConnectedThread(socket).write(message.toByteArray())
                    }
                } catch (e: IOException) {
                    Toast.makeText(
                        requireContext(),
                        "Verbindung nicht erfolgreich",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.textView.visibility = View.VISIBLE
                    binding.rvBluetoothDevices.visibility = View.VISIBLE
                    binding.btnEndSearch.visibility = View.VISIBLE
                    binding.btnStartSearch.visibility = View.VISIBLE
                }
            }
        }

    }

    //Klasse um Daten an ein verbundenes Gerät zu senden oder Daten zu empfangen
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024)

        //Daten einlesen
        override fun run() {
            var numBytes: Int

            //Inputstream abhören bis ein Fehler entsteht
            while (true) {
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d("geu", "Input stream was disconnected", e)
                    break
                }

            }
        }

        //Aufrufen um Daten an ein BT-Gerät zu senden
        fun write(bytes: ByteArray) {
            try {
                TimeUnit.SECONDS.sleep(1L) //Wartezeit um Timingproblem zu beheben
                mmOutStream.write(bytes)

            } catch (e: IOException) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    Log.e("geu", "Error occurred when sending data", e)
                    Toast.makeText(
                        requireContext(),
                        "Daten konnten nicht an den ESP gesendet werden",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.textView.visibility = View.VISIBLE
                    binding.rvBluetoothDevices.visibility = View.VISIBLE
                    binding.btnEndSearch.visibility = View.VISIBLE
                    binding.btnStartSearch.visibility = View.VISIBLE
                }
                return
            }

            closeBluetoothSocket()

        }

        fun closeBluetoothSocket() {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                try {
                    mmSocket.close()
                } catch (e: IOException) {
                    Log.e("geu", "Could not close the connect socket", e)
                    binding.textView.visibility = View.VISIBLE
                    binding.rvBluetoothDevices.visibility = View.VISIBLE
                    binding.btnEndSearch.visibility = View.VISIBLE
                    binding.btnStartSearch.visibility = View.VISIBLE
                }
            }
        }
    }

    //Hinzufügen der Pflanze zur Liste und Liste auf dem Handy speichern
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