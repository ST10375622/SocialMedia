package com.fake.socialmedia

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class PostActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2
    private val REQUEST_CAMERA_PERMISSION = 101

    private lateinit var postImage: ImageView
    private lateinit var caption: EditText
    private lateinit var bottomNav: BottomNavigationView
    private var selectedBitmap: Bitmap? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postsList = mutableListOf<Post>()

    private var username: String = "UnknownUser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        username = intent.getStringExtra("username") ?: "UnknownUser"

        postImage = findViewById(R.id.postImage)
        caption = findViewById(R.id.caption)
        recyclerView = findViewById(R.id.recyclerViewPosts)
        bottomNav = findViewById(R.id.bottomNav)

        recyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(postsList)
        recyclerView.adapter = postAdapter

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

        findViewById<Button>(R.id.btnSelectImage).setOnClickListener { openGallery() }

        findViewById<Button>(R.id.btnCaptureImage).setOnClickListener {
            Toast.makeText(this, "Capture button clicked", Toast.LENGTH_SHORT).show()
            checkCameraPermissionAndOpenCamera()
        }

        findViewById<Button>(R.id.btnUploadPost).setOnClickListener { uploadPost() }

        loadPosts()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    selectedBitmap = data?.extras?.get("data") as? Bitmap
                    postImage.setImageBitmap(selectedBitmap)
                }
                REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    val inputStream = contentResolver.openInputStream(imageUri!!)
                    selectedBitmap = BitmapFactory.decodeStream(inputStream)
                    postImage.setImageBitmap(selectedBitmap)
                }
            }
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun uploadPost() {
        if (selectedBitmap == null || caption.text.isBlank()) {
            Toast.makeText(this, "Please select an image and enter a caption.", Toast.LENGTH_SHORT).show()
            return
        }

        val imageBase64 = encodeImageToBase64(selectedBitmap!!)

        val post = Post(
            imageBase64 = imageBase64,
            caption = caption.text.toString(),
            username = username,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("posts")
            .add(post)
            .addOnSuccessListener {
                Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show()

                selectedBitmap = null
                postImage.setImageResource(R.mipmap.profile_place_holder_foreground)
                caption.text.clear()

                postsList.add(0, post)
                postAdapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error uploading post: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
    }
}
