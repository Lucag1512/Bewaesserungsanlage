package com.example.t3100.adapter

import android.annotation.SuppressLint
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

    @SuppressLint("SetTextI18n")

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.binding.apply {
            tvName.text = "Pflanzenname: ${plants[position].name}"

            tvValve.text = "Ventil: ${plants[position].valve}"

            ivEdit.setOnClickListener {
                clickListener.onEditClick(position)
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
        fun onEditClick(pos: Int)
        fun onDeleteClick(pos: Int)
    }
}