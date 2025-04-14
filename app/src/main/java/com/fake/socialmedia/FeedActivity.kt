package com.fake.socialmedia

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class FeedActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postsList = mutableListOf<Post>()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var firestore: FirebaseFirestore
    private var username: String = "UnknownUser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        username = intent.getStringExtra("username") ?: "UnknownUser"
        recyclerView = findViewById(R.id.recyclerViewPosts)
        bottomNav = findViewById(R.id.bottomNav)
        recyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(postsList)
        recyclerView.adapter = postAdapter

        firestore = FirebaseFirestore.getInstance()

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("username", username)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.Profile -> {
                    Toast.makeText(this, "coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.upload -> {
                    val intent = Intent(this, PostActivity::class.java)
                    intent.putExtra("username", username)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        loadPosts()
    }

    private fun loadPosts() {
        postsList.clear()
        firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val post = doc.toObject(Post::class.java)
                    postsList.add(post)
                }
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
    }
}
