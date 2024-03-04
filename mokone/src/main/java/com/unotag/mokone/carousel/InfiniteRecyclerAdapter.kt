package com.unotag.mokone.carousel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unotag.mokone.R
import com.unotag.mokone.carousel.data.CarouselContent

class InfiniteRecyclerAdapter(originalList: List<CarouselContent>) : RecyclerView.Adapter<InfiniteRecyclerAdapter.InfiniteRecyclerViewHolder>() {

    private val newList: List<CarouselContent> =
        listOf(originalList.last()) + originalList + listOf(originalList.first())

    class InfiniteRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: CarouselContent) {
            val carouselImageView: ImageView = itemView.findViewById(R.id.carouselImageView)

            Glide.with(itemView.context)
                .load(item.url)
                .centerInside()
                .into(carouselImageView)
        }

        companion object {
            fun from(parent: ViewGroup) : InfiniteRecyclerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val itemView = layoutInflater.inflate(R.layout.item_carousel,
                    parent, false)
                return InfiniteRecyclerViewHolder(itemView)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfiniteRecyclerViewHolder {
        return InfiniteRecyclerViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: InfiniteRecyclerViewHolder, position: Int) {
        holder.bind(newList[position])
    }

    override fun getItemCount(): Int {
        return newList.size
    }

}