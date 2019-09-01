package com.atsera.testapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.atsera.testapp.Login.Login

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(this, Login::class.java)
// To pass any data to next activity
        //intent.putExtra("keyIdentifier", value)
// start your next activity
        startActivity(intent)
    }
}
