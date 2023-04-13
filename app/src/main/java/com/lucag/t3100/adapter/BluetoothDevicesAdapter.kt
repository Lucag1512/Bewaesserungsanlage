package com.lucag.t3100.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.lucag.t3100.databinding.ItemBluetoothdevicesBinding

class BluetoothDevicesAdapter(
    var bluetoothDevices : List<BluetoothDevice>,
    val clickListener: ItemClickListener
) : RecyclerView.Adapter<BluetoothDevicesAdapter.BluetoothDeviceViewHolder>(){

    inner class BluetoothDeviceViewHolder(val binding : ItemBluetoothdevicesBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothDeviceViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemBluetoothdevicesBinding.inflate(layoutInflater, parent, false)
        return BluetoothDeviceViewHolder(binding)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: BluetoothDeviceViewHolder, position: Int) {
        holder.binding.apply {
            //Name und Adresse in Listenelement übernehmen
            tvBTMacAdress.text = "MAC-Adresse: ${bluetoothDevices[position].address}"
            tvBTName.text = bluetoothDevices[position].name

            root.setOnClickListener {
                clickListener.onItemClick(bluetoothDevices[position])
            }
        }
    }

    override fun getItemCount(): Int {
       return bluetoothDevices.size
    }

    //Interface Methoden
    interface ItemClickListener {
        fun onItemClick(device: BluetoothDevice)
    }

}