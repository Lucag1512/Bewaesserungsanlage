package com.example.t3100.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.t3100.data.Plant
import com.example.t3100.databinding.ItemPlantBinding
import com.example.t3100.ui.main.PlantListFragment


class PlantAdapter(
    var plants: List<Plant>,
    var clickListener: PlantListFragment
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>(){

    inner class PlantViewHolder(val binding : ItemPlantBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemPlantBinding.inflate(layoutInflater, parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.binding.apply {
            tvName.text = "Pflanzenname: ${plants[position].name}"
            tvWater.text = "Tägliche Wassermenge: ${plants[position].water}mL"
            tvValve.text = "Ventil: ${plants[position].valve}"
            if(plants[position].minute < 10 && plants[position].hour < 10){
                tvWateringTime.text ="Bewässerungszeitpunkt: 0${plants[position].hour}:0${plants[position].minute}"
            } else if(plants[position].hour < 10){
                tvWateringTime.text ="Bewässerungszeitpunkt: 0${plants[position].hour}:${plants[position].minute}"
            }else if (plants[position].minute <10){
                tvWateringTime.text ="Bewässerungszeitpunkt: ${plants[position].hour}:0${plants[position].minute}"
            } else {
                tvWateringTime.text = "Bewässerungszeitpunkt: ${plants[position].hour}:${plants[position].minute}"
            }
            ivDelete.setOnClickListener {
                clickListener.onDeleteClick(position)
            }

        }

    }

    override fun getItemCount(): Int {
       return plants.size
    }

    //Define your Interface method here
    interface ItemClickListener {
        fun onDeleteClick(pos: Int)
    }
}