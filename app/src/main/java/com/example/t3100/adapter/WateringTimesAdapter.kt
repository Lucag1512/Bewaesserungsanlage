package com.example.t3100.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.t3100.data.WateringElement
import com.example.t3100.databinding.ItemWateringtimesBinding
import com.example.t3100.ui.main.AddPlantFragment
import com.example.t3100.ui.main.EditPlantFragment

class WateringTimesAdapter(
    var wateringTimes: List<WateringElement>,
    var clickListener: ItemClickListener,
    var calibrationValue: Double
) : RecyclerView.Adapter<WateringTimesAdapter.WateringTimesViewHolder>(){

    inner class WateringTimesViewHolder(val binding : ItemWateringtimesBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WateringTimesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemWateringtimesBinding.inflate(layoutInflater, parent, false)
        return WateringTimesViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")

    override fun onBindViewHolder(holder: WateringTimesViewHolder, position: Int) {
        holder.binding.apply {

            //Wassermenge und Uhrzeit der Bewässerung in Listenelement übernehmen
            tvWaterAmount.text = "${((wateringTimes[position].water)*calibrationValue).toInt()} ml"

            if(wateringTimes[position].minute < 10 && wateringTimes[position].hour < 10){
                tvWateringTime.text ="Bewässerungszeitpunkt: 0${wateringTimes[position].hour}:0${wateringTimes[position].minute} Uhr"
            } else if(wateringTimes[position].hour < 10){
                tvWateringTime.text ="Bewässerungszeitpunkt: 0${wateringTimes[position].hour}:${wateringTimes[position].minute} Uhr"
            }else if (wateringTimes[position].minute <10){
                tvWateringTime.text ="Bewässerungszeitpunkt: ${wateringTimes[position].hour}:0${wateringTimes[position].minute} Uhr"
            } else {
                tvWateringTime.text = "Bewässerungszeitpunkt: ${wateringTimes[position].hour}:${wateringTimes[position].minute} Uhr"
            }

            ivEdit.setOnClickListener {
                clickListener.onEditClick(position)
            }

            ivDelete.setOnClickListener {
                clickListener.onDeleteClick(position)
            }
        }
    }

    override fun getItemCount(): Int {
       return wateringTimes.size
    }

    //Interface Methoden
    interface ItemClickListener {
        fun onEditClick(pos: Int)
        fun onDeleteClick(pos: Int)
    }

}