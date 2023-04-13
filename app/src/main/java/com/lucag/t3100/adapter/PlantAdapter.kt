package com.lucag.t3100.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lucag.t3100.R
import com.lucag.t3100.data.Plant
import com.lucag.t3100.databinding.ItemPlantBinding
import com.lucag.t3100.ui.main.PlantListFragment

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
            //Werte der Pflanze eintragen
            tvName.text = "Pflanzenname: ${plants[position].name}"
            tvValve.text = "Ventil: ${plants[position].valve}"

            //Prüfung: Existiert Bild der Pflanze in Datenbank
            //Wenn Ja, Bild einfügen
            if(plants[position].name?.lowercase()?.contains("chili")  == true){
                ivPlantPicture.setImageResource(R.drawable.chilli)
            }
            else if(plants[position].name?.lowercase()?.contains("gurke")  == true){
                ivPlantPicture.setImageResource(R.drawable.cucumber)
            }
            else if(plants[position].name?.lowercase()?.contains("salat")  == true){
                ivPlantPicture.setImageResource(R.drawable.lettuce)
            }
            else if(plants[position].name?.lowercase()?.contains("paprika")  == true){
                ivPlantPicture.setImageResource(R.drawable.paprika)
            }
            else if(plants[position].name?.lowercase()?.contains("radieschen") == true){
                ivPlantPicture.setImageResource(R.drawable.radish)
            }
            else if(plants[position].name?.lowercase()?.contains("erdbeere")  == true){
                ivPlantPicture.setImageResource(R.drawable.strawberry)
            }
            else if(plants[position].name?.lowercase()?.contains("tomate")  == true){
                ivPlantPicture.setImageResource(R.drawable.tomato)
            }
            else if(plants[position].name?.lowercase()?.contains("zucchini")  == true){
                ivPlantPicture.setImageResource(R.drawable.zucchini)
            }
            else{
                ivPlantPicture.setImageResource(R.drawable.plant)
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
       return plants.size
    }

    //Interface Methoden
    interface ItemClickListener {
        fun onEditClick(pos: Int)
        fun onDeleteClick(pos: Int)
    }
}