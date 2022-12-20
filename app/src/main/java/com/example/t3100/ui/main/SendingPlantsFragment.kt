package com.example.t3100.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.adapter.BluetoothDevicesAdapter
import com.example.t3100.data.ParsedDate
import com.example.t3100.data.PlantHeader
import com.example.t3100.databinding.FragmentSendingplantsBinding
import com.example.t3100.viewmodel.BluetoothViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class SendingPlantsFragment : Fragment() {

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

    val job = Job()

    private lateinit var viewModel: BluetoothViewModel
    private lateinit var binding: FragmentSendingplantsBinding

    private var bluetoothAdapter: BluetoothAdapter? = null

    private lateinit var adapter: BluetoothDevicesAdapter

    private val args: SendingPlantsFragmentArgs by navArgs()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(BluetoothViewModel::class.java)
    }

    //Binding für einfacheren Zugriff auf Buttons, TV aus xml File
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.title = "Verfügbare Mikrocontroller"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_sendingplants, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Adapter anlegen, entspricht BT Adapter des lokalen Gerätes
        val bluetoothManager: BluetoothManager? =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        viewModel.plantList = args.plantList.toList()
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        adapter = BluetoothDevicesAdapter(viewModel.bluetoothDevices, object :
            BluetoothDevicesAdapter.ItemClickListener {
            override fun onItemClick(device: BluetoothDevice) {

                AlertDialog.Builder(requireContext()).create().apply {
                    setTitle("Achtung")
                    setMessage("Möchten sie die Daten übertragen")
                    setButton(AlertDialog.BUTTON_NEGATIVE, "Abbrechen") { dialog, p1 ->
                        dialog.dismiss()
                    }
                    setButton(AlertDialog.BUTTON_POSITIVE, "OK") { p0, p1 ->
                        p0.dismiss()

                        binding.textView.visibility = View.INVISIBLE
                        binding.rvBluetoothDevices.visibility = View.INVISIBLE
                        binding.btnEndSearch.visibility = View.INVISIBLE
                        binding.btnStartSearch.visibility = View.INVISIBLE
                        binding.loadingBarSendingData.visibility = View.VISIBLE
                        binding.tvSendingData.visibility = View.VISIBLE

                        val calendar = Calendar.getInstance()
                        val parsedDate = ParsedDate(
                            calendar.get(Calendar.SECOND),
                            calendar.get(Calendar.MINUTE),
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.DAY_OF_MONTH),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.YEAR)
                        )

                        val plantHeader = PlantHeader(viewModel.plantList, parsedDate)
                        val gson = Gson()
                        val plantString = gson.toJson(plantHeader)
                        ConnectThread(device).connectAndSend(plantString)

                    }
                    show()
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

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
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
                    val deviceName = device?.name
                    device?.address // MAC address

                    if (deviceName?.contains("ESP") == true && !viewModel.bluetoothDevices.contains(
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

        @SuppressLint("SuspiciousIndentation")
        fun connectAndSend(message: String) {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()
            binding.loadingBarSearch.visibility = View.GONE

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        socket.connect()

                        // The connection attempt succeeded. Perform work associated with
                        // the connection in a separate thread.
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
                    binding.loadingBarSendingData.visibility = View.INVISIBLE
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
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
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
                    binding.textView.visibility = View.VISIBLE
                    binding.rvBluetoothDevices.visibility = View.VISIBLE
                    binding.btnEndSearch.visibility = View.VISIBLE
                    binding.btnStartSearch.visibility = View.VISIBLE
                    binding.loadingBarSendingData.visibility = View.INVISIBLE
                }
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
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                try {
                    mmSocket.close()
                    Toast.makeText(
                        requireContext(),
                        "Daten erfolgreich gesendet",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()

                } catch (e: IOException) {
                    Log.e("geu", "Could not close the connect socket", e)
                    binding.textView.visibility = View.VISIBLE
                    binding.rvBluetoothDevices.visibility = View.VISIBLE
                    binding.btnEndSearch.visibility = View.VISIBLE
                    binding.btnStartSearch.visibility = View.VISIBLE
                    binding.loadingBarSendingData.visibility = View.INVISIBLE
                }
            }
        }
    }


}