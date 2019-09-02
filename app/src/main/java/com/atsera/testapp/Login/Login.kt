package com.atsera.testapp.Login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.atsera.testapp.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*

import java.util.concurrent.TimeUnit


class Login : AppCompatActivity() {


    lateinit var text:TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.atsera.testapp.R.layout.activity_login)
        var bundle = intent.extras
        var count =bundle?.get("count")

        text = findViewById(R.id.textView)

        text.text = "welcome back for ${count} time"


    }

}
