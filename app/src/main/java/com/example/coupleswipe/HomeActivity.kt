package com.example.coupleswipe
import Category
import CategoryAdapter
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coupleswipe.R
import com.example.coupleswipe.SwipeActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val categories = listOf(
            Category("Food", R.drawable.food_icon),
            Category("Movies", R.drawable.food_icon),
            Category("Travel", R.drawable.food_icon),
            Category("Music", R.drawable.food_icon),
            Category("Sports", R.drawable.food_icon),
            Category("Gifts", R.drawable.food_icon),
            Category("Activities", R.drawable.food_icon)
        )

        val adapter = CategoryAdapter(categories) { category ->
            // Handle category click
            openSwipeScreen(category)
        }
        recyclerView.adapter = adapter
    }

    private fun openSwipeScreen(category: Category) {
        val intent = Intent(this, SwipeActivity::class.java)
        intent.putExtra("CATEGORY_NAME", category.name)
        startActivity(intent)
    }
}
