package ru.ytkab0bp.sapil

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        PlaceholderRepo.getComments(10) { response ->
            window.decorView.post {
                Toast.makeText(this, "Got comments: $response", Toast.LENGTH_SHORT).show()
            }
        }
        PlaceholderRepo.getPosts { response -> println("Posts$response") }
    }
}
