package com.example.coupleswipe.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coupleswipe.R
import com.example.coupleswipe.adapter.CategoryAdapter
import com.example.coupleswipe.model.Category
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryArrayList:ArrayList<Category>
    private lateinit var  db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        categoryArrayList = arrayListOf()
        categoryAdapter = CategoryAdapter(categoryArrayList) { category ->
            openNewGameScreen(category)
        }
        recyclerView.adapter = categoryAdapter


        EventChangeListener()
    }

    private fun EventChangeListener(){
        db=FirebaseFirestore.getInstance();
        Log.d("event","event")
        db.collection("categories").
                addSnapshotListener(object:EventListener<QuerySnapshot>{
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onEvent(
                        value: QuerySnapshot?,
                        error: FirebaseFirestoreException?
                    ) {
                        if(error!=null){
                            Log.e("Firestore error ", error.message.toString())
                            return
                        }
                        for(dc:DocumentChange in value?.documentChanges!! ){
                            if(dc.type == DocumentChange.Type.ADDED){
                                categoryArrayList.add(dc.document.toObject(Category::class.java))
                            }
                        }
                        categoryAdapter.notifyDataSetChanged()
                    }
                })
    }


    private fun openNewGameScreen(category: Category) {
        if (!category.isValid()) {
            Log.e("HomeActivity", "Invalid category - missing ID or name")
            Toast.makeText(this, "Invalid category selected", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, NewGame::class.java).apply {
            putExtra("CATEGORY_NAME", category.name!!)
            Toast.makeText(this@HomeActivity,category.name.toString(),Toast.LENGTH_SHORT).show()
        }
        startActivity(intent)
    }
}