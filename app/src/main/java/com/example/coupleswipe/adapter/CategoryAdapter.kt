package com.example.coupleswipe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.coupleswipe.R
import com.example.coupleswipe.model.Category

class CategoryAdapter(
    var categoryList: List<Category>, // Make this mutable
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>(){

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryImage: ImageView = view.findViewById(R.id.category_image)
        val categoryName: TextView = view.findViewById(R.id.category_name)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]

        holder.categoryName.text = category.name


        Glide.with(holder.itemView.context)
            .load(category.imageUrl)
            .placeholder(R.drawable.default_category_icon)
            .error(R.drawable.default_category_icon)
            .into(holder.categoryImage)

        holder.itemView.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categoryList.size
}