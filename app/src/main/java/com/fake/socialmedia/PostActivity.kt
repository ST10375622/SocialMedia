package com.fake.socialmedia

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream

class PostActivity : AppCompatActivity() {

    private lateinit var postImage: ImageView
    private lateinit var caption: EditText
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnImage: Button
    private var selectedBitmap: Bitmap? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var username: String = "UnknownUser"

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data

            if (data?.data != null){
                val uri = data.data!!
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedBitmap = bitmap
                postImage.setImageBitmap(bitmap)
            }else {
                val bitmap = data?.extras?.get("data") as? Bitmap
                bitmap?.let {
                    selectedBitmap = it
                    postImage.setImageBitmap(it)
                }
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)



        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        username = intent.getStringExtra("username") ?: "UnknownUser"

        postImage = findViewById(R.id.postImage)
        caption = findViewById(R.id.caption)
        bottomNav = findViewById(R.id.bottomNav)
        btnImage = findViewById(R.id.btnImage)




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

        btnImage.setOnClickListener {
            showImagePicker()
        }

        findViewById<Button>(R.id.btnUploadPost).setOnClickListener { uploadPost() }

        findViewById<Button>(R.id.btnGoToFeed).setOnClickListener {
            val intent = Intent(this, FeedActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

    }

    private fun showImagePicker()
    {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val chooserIntent = Intent.createChooser(pickIntent, "Select Image")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePhotoIntent))
        imagePickerLauncher.launch(chooserIntent)
    }


    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
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


            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error uploading post: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}
