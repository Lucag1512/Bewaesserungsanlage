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
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.adapter.BluetoothDevicesAdapter
import com.example.t3100.data.ManualWateringElements
import com.example.t3100.data.ParsedDataManual
import com.example.t3100.databinding.FragmentManualwateringBinding
import com.example.t3100.viewmodel.BluetoothViewModel
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class ManualWateringFragment : Fragment() {

    //Companion object verwendung der Variablen in Klasse
    companion object {

        /* Aktuell nicht verwendet
        // Defines several constants used when transmitting messages between the
        // service and the UI.
        val MESSAGE_READ: Int = 0
        val MESSAGE_WRITE: Int = 1
        val MESSAGE_TOAST: Int = 2
        */

    }

    private var lastDevice: BluetoothDevice? = null

    private lateinit var viewModel: BluetoothViewModel
    private lateinit var binding: FragmentManualwateringBinding

    private var bluetoothAdapter: BluetoothAdapter? = null

    private lateinit var adapter: BluetoothDevicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(BluetoothViewModel::class.java)

        requireActivity().onBackPressedDispatcher.addCallback(this) {

            lastDevice?.let {
                val manualWateringElements = ParsedDataManual(ManualWateringElements(0, 0, 0, 0))
                val gson = Gson()
                val manualWateringElementsJson = gson.toJson(manualWateringElements)
                ConnectThread(it).connectAndSend(manualWateringElementsJson)
                lastDevice = null
            }
            findNavController().popBackStack()

        }
    }

    //Binding für einfacheren Zugriff auf Buttons, TV aus xml File
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        activity?.title = "Manuelle Steuerung"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_manualwatering, container, false
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

        //Variablen der einzelnen Elemente
        var pump = 0
        var valve1 = 0
        var valve2 = 0
        var valve3 = 0
        var manualWateringElements: ParsedDataManual

        adapter = BluetoothDevicesAdapter(
            viewModel.bluetoothDevices,
            object : BluetoothDevicesAdapter.ItemClickListener {
                override fun onItemClick(device: BluetoothDevice) {

                    stopSearchDevices()

                    //Benötigten Buttons sichtbar machen und Gerätesuche beenden
                    binding.tvChoose.visibility = View.INVISIBLE
                    binding.rvBluetoothDevices.visibility = View.INVISIBLE
                    binding.btnStartSearch.visibility = View.INVISIBLE
                    binding.btnEndSearch.visibility = View.INVISIBLE
                    binding.tvConnected.visibility = View.VISIBLE
                    binding.btnPump.visibility = View.VISIBLE
                    binding.btnValve1.visibility = View.VISIBLE
                    binding.btnValve2.visibility = View.VISIBLE
                    binding.btnValve3.visibility = View.VISIBLE
                    binding.btnPump.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.Green
                        )
                    )
                    binding.btnValve1.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.Green
                        )
                    )
                    binding.btnValve2.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.Green
                        )
                    )
                    binding.btnValve3.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.Green
                        )
                    )

                    lastDevice = device

                    binding.btnPump.setOnClickListener {

                        if (pump == 0) {

                            if (valve1 == 0 && valve2 == 0 && valve3 == 0) {
                                AlertDialog.Builder(requireContext()).create().apply {
                                    setTitle("Hinweis")
                                    setMessage("Aus Sicherheitsgründen muss zum Ansteuern der Pumpe ein Ventil geöffnet sein! Möchten sie Ventil 1 und die Pumpe ansteuern?")
                                    setButton(
                                        AlertDialog.BUTTON_NEGATIVE,
                                        "Abbrechen"
                                    ) { dialog, p1 ->
                                        dialog.dismiss()
                                    }
                                    setButton(AlertDialog.BUTTON_POSITIVE, "Ja") { p0, p1 ->
                                        p0.dismiss()

                                        pump = 255
                                        valve1 = 1
                                        manualWateringElements = ParsedDataManual(
                                            ManualWateringElements(
                                                pump, valve1, valve2, valve3
                                            )
                                        )

                                        val gson = Gson()
                                        val manualWateringElementsJson =
                                            gson.toJson(manualWateringElements)
                                        ConnectThread(device).connectAndSend(
                                            manualWateringElementsJson
                                        )

                                        binding.btnPump.text = "Pumpe Ausschalten"
                                        binding.btnPump.setBackgroundColor(
                                            ContextCompat.getColor(
                                                requireContext(), R.color.Red
                                            )
                                        )
                                        binding.loadingBarPumpOn.visibility = View.VISIBLE

                                        binding.btnValve1.text = "Ventil 1 schließen"
                                        binding.btnValve1.setBackgroundColor(
                                            ContextCompat.getColor(
                                                requireContext(), R.color.Red
                                            )
                                        )
                                        binding.loadingBarValve1Open.visibility = View.VISIBLE
                                    }
                                    show()
                                }
                            } else {
                                pump = 255
                                manualWateringElements = ParsedDataManual(
                                    ManualWateringElements(
                                        pump, valve1, valve2, valve3
                                    )
                                )

                                val gson = Gson()
                                val manualWateringElementsJson = gson.toJson(manualWateringElements)
                                ConnectThread(device).connectAndSend(manualWateringElementsJson)

                                binding.btnPump.text = "Pumpe Ausschalten"
                                binding.btnPump.setBackgroundColor(
                                    ContextCompat.getColor(
                                        requireContext(), R.color.Red
                                    )
                                )
                                binding.loadingBarPumpOn.visibility = View.VISIBLE
                            }


                        } else if (pump == 255) {
                            pump = 0
                            manualWateringElements = ParsedDataManual(
                                ManualWateringElements(
                                    pump,
                                    valve1,
                                    valve2,
                                    valve3
                                )
                            )

                            val gson = Gson()
                            val manualWateringElementsJson = gson.toJson(manualWateringElements)
                            ConnectThread(device).connectAndSend(manualWateringElementsJson)

                            binding.btnPump.text = "Pumpe Einschalten"
                            binding.btnPump.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.Green
                                )
                            )
                            binding.loadingBarPumpOn.visibility = View.INVISIBLE
                        }

                    }


                    binding.btnValve1.setOnClickListener {
                        if (valve1 == 0) {
                            valve1 = 1
                            manualWateringElements = ParsedDataManual(
                                ManualWateringElements(
                                    pump,
                                    valve1,
                                    valve2,
                                    valve3
                                )
                            )

                            val gson = Gson()
                            val manualWateringElementsJson = gson.toJson(manualWateringElements)
                            ConnectThread(device).connectAndSend(manualWateringElementsJson)

                            binding.btnValve1.text = "Ventil 1 schließen"
                            binding.btnValve1.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.Red
                                )
                            )
                            binding.loadingBarValve1Open.visibility = View.VISIBLE
                        } else if (valve1 == 1 && pump == 255 && valve2 == 0 && valve3 == 0) {
                            AlertDialog.Builder(requireContext()).create().apply {
                                setTitle("Achtung")
                                setMessage("Die Pumpe darf nicht alleine geöffnet sein")
                                setButton(AlertDialog.BUTTON_NEGATIVE, "Abbrechen") { dialog, p1 ->
                                    dialog.dismiss()
                                }
                                show()
                            }
                        } else {
                            valve1 = 0
                            manualWateringElements = ParsedDataManual(
                                ManualWateringElements(
                                    pump,
                                    valve1,
                                    valve2,
                                    valve3
                                )
                            )

                            val gson = Gson()
                            val manualWateringElementsJson = gson.toJson(manualWateringElements)
                            ConnectThread(device).connectAndSend(manualWateringElementsJson)

                            binding.btnValve1.text = "Ventil 1 öffnen"
                            binding.btnValve1.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.Green
                                )
                            )
                            binding.loadingBarValve1Open.visibility = View.INVISIBLE
                        }
                    }

                    binding.btnValve2.setOnClickListener {
                        if (valve2 == 0) {
                            valve2 = 1
                            manualWateringElements = ParsedDataManual(
                                ManualWateringElements(
                                    pump,
                                    valve1,
                                    valve2,
                                    valve3
                                )
                            )

                            val gson = Gson()
                            val manualWateringElementsJson = gson.toJson(manualWateringElements)
                            ConnectThread(device).connectAndSend(manualWateringElementsJson)

                            binding.btnValve2.text = "Ventil 2 schließen"
                            binding.btnValve2.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.Red
                                )
                            )
                            binding.loadingBarValve2Open.visibility = View.VISIBLE
                        } else if (valve2 == 1 && pump == 255 && valve1 == 0 && valve3 == 0) {
                            AlertDialog.Builder(requireContext()).create().apply {
                                setTitle("Achtung")
                                setMessage("Die Pumpe darf nicht alleine geöffnet sein")
                                setButton(AlertDialog.BUTTON_NEGATIVE, "Abbrechen") { dialog, p1 ->
                                    dialog.dismiss()
                                }
                                show()
                            }
                        } else {
                            valve2 = 0
                            manualWateringElements = ParsedDataManual(
                                ManualWateringElements(
                                    pump,
                                    valve1,
                                    valve2,
                                    valve3
                                )
                            )

                            val gson = Gson()
                            val manualWateringElementsJson = gson.toJson(manualWateringElements)
                            ConnectThread(device).connectAndSend(manualWateringElementsJson)

                            binding.btnValve2.text = "Ventil 2 öffnen"
                            binding.btnValve2.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.Green
                                )
                            )
                            binding.loadingBarValve2Open.visibility = View.INVISIBLE
                        }
                    }

                    binding.btnValve3.setOnClickListener {

                        if (valve3 == 0) {
                            valve3 = 1
                            manualWateringElements = ParsedDataManual(
                                ManualWateringElements(
                                    pump,
                                    valve1,
                                    valve2,
                                    valve3
                                )
                            )

                            val gson = Gson()
                            val manualWateringElementsJson = gson.toJson(manualWateringElements)
                            ConnectThread(device).connectAndSend(manualWateringElementsJson)

                            binding.btnValve3.text = "Ventil 3 schließen"
                            binding.btnValve3.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.Red
                                )
                            )
                            binding.loadingBarValve3Open.visibility = View.VISIBLE
                        } else if (valve3 == 1 && pump == 255 && valve1 == 0 && valve2 == 0) {
                            AlertDialog.Builder(requireContext()).create().apply {
                                setTitle("Achtung")
                                setMessage("Die Pumpe darf nicht alleine geöffnet sein")
                                setButton(AlertDialog.BUTTON_NEGATIVE, "Abbrechen") { dialog, p1 ->
                                    dialog.dismiss()
                                }
                                show()
                            }
                        } else {
                            valve3 = 0
                            manualWateringElements = ParsedDataManual(
                                ManualWateringElements(
                                    pump,
                                    valve1,
                                    valve2,
                                    valve3
                                )
                            )

                            val gson = Gson()
                            val manualWateringElementsJson = gson.toJson(manualWateringElements)
                            ConnectThread(device).connectAndSend(manualWateringElementsJson)

                            binding.btnValve3.text = "Ventil 3 öffnen"
                            binding.btnValve3.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.Green
                                )
                            )
                            binding.loadingBarValve3Open.visibility = View.INVISIBLE
                        }
                    }
                }
            })

        binding.rvBluetoothDevices.adapter = adapter
        binding.rvBluetoothDevices.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            (binding.rvBluetoothDevices.layoutManager as LinearLayoutManager).orientation
        )
        binding.rvBluetoothDevices.addItemDecoration(dividerItemDecoration)

        //Verfügbare BT Geräte in der Nähe suchen
        binding.btnStartSearch.setOnClickListener {
            startSearchDevices()
        }

        binding.btnEndSearch.setOnClickListener {
            stopSearchDevices()
        }

    }

    override fun onResume() {
        super.onResume()

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(receiver, filter)
        //LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(receiver, filter)
    }

    //BT reciever deaktivieren bei Schließen der App
    override fun onPause() {
        super.onPause()
        // Don't forget to unregister the ACTION_FOUND receiver.
        //LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(receiver)
        requireActivity().unregisterReceiver(receiver)
    }

    //Suche nach Geräte starten
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

    // Create a BroadcastReceiver for ACTION_FOUND.
    // Nach Geräten suchen und Name sowie MAC-Adresse in Variablen speichern
    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
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
            device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
        }

        fun connectAndSend(message: String) {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    ConnectedThread(socket).write(message.toByteArray())

                } catch (e: IOException) {
                    Toast.makeText(
                        requireContext(), "Verbindung nicht erfolgreich", Toast.LENGTH_LONG
                    ).show()

                }
            }
        }

    }

    //Klasse um Daten an ein verbundenes Gerät zu senden oder Daten zu empfangen
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d("geu", "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                /* val readMsg = handler.obtainMessage(
                     MESSAGE_READ, numBytes, -1,
                     mmBuffer)
                 readMsg.sendToTarget()*/
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                TimeUnit.SECONDS.sleep(1L)
                mmOutStream.write(bytes)

            } catch (e: IOException) {
                Log.e("geu", "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                /*val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
            val bundle = Bundle().apply {
                putString("toast", "Couldn't send data to the other device")
            }
            writeErrorMsg.data = bundle
            handler.sendMessage(writeErrorMsg)*/
                Toast.makeText(
                    requireContext(),
                    "Daten konnten nicht an den ESP gesendet werden",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            closeBluetoothSocket()

            /* // Share the sent message with the UI activity.
             val writtenMsg = handler.obtainMessage(
                 MESSAGE_WRITE, -1, -1, mmBuffer)
             writtenMsg.sendToTarget()*/
        }

        // Call this method from the main activity to shut down the connection.
        fun closeBluetoothSocket() {
            try {
                mmSocket.close()

            } catch (e: IOException) {
                Log.e("geu", "Could not close the connect socket", e)

            }
        }
    }


}