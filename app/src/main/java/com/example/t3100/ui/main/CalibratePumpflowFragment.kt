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

    private var choosenValve: Int = 0

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

        //Adapter anlegen, entspricht BT Adapter des lokalen Gerätes
        val bluetoothManager: BluetoothManager? =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        //Verbindung mit gerät aufbauen
        adapter = BluetoothDevicesAdapter(viewModel.bluetoothDevices, object :
            BluetoothDevicesAdapter.ItemClickListener {
            override fun onItemClick(device: BluetoothDevice) {
                stopSearchDevices()

                binding.textView.visibility = View.INVISIBLE
                binding.rvBluetoothDevices.visibility = View.INVISIBLE
                binding.btnEndSearch.visibility = View.INVISIBLE
                binding.btnStartSearch.visibility = View.INVISIBLE

                AlertDialog.Builder(requireContext()).create().apply {
                    setTitle("Hinweis")
                    setMessage(
                        "Nach Auswahl des entsprechenden Ventils wird das Ventil für 10s " +
                                "angesteuert. Anschließend bitte gesamt geflossene Menge Wasser eintragen"
                    )
                    setButton(AlertDialog.BUTTON_POSITIVE, "OK") { dialog, p1 ->
                        dialog.dismiss()
                    }
                    show()
                }

                //Buttons zur Ventilauswahl einblenden
                binding.tvChooseValve.visibility = View.VISIBLE
                binding.btnValve1.visibility = View.VISIBLE
                binding.btnValve2.visibility = View.VISIBLE
                binding.btnValve3.visibility = View.VISIBLE

                binding.btnValve1.setOnClickListener {
                    //Variable zum JSON Format konvertieren
                    val calibrate = ParsedCalibrate(true, 1)
                    val gson = Gson()
                    val calibrateJson = gson.toJson(calibrate)
                    ConnectThread(device).connectAndSend(calibrateJson)

                    choosenValve = 1
                }

                binding.btnValve2.setOnClickListener {
                    //Variable zum JSON Format konvertieren
                    val calibrate = ParsedCalibrate(true, 2)
                    val gson = Gson()
                    val calibrateJson = gson.toJson(calibrate)
                    ConnectThread(device).connectAndSend(calibrateJson)

                    choosenValve = 2
                }

                binding.btnValve3.setOnClickListener {
                    //Variable zum JSON Format konvertieren
                    val calibrate = ParsedCalibrate(true, 3)
                    val gson = Gson()
                    val calibrateJson = gson.toJson(calibrate)
                    ConnectThread(device).connectAndSend(calibrateJson)

                    choosenValve = 3
                }
            }
        })

        binding.rvBluetoothDevices.adapter = adapter

        //Trennlinie zwischen den angezeigten BT-Geräten einfügen
        binding.rvBluetoothDevices.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            (binding.rvBluetoothDevices.layoutManager as LinearLayoutManager).orientation
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

                    findNavController().navigate(
                        CalibratePumpflowFragmentDirections.actionCalibratePumpflowFragmentToCalibratePumpflowValveFragment(
                            choosenValve
                        )
                    )
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