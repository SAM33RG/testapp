package com.atsera.testapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseError
import com.google.firebase.database.*
import java.io.File
import id.zelory.compressor.Compressor
import io.reactivex.internal.util.HalfSerializer.onComplete
import org.w3c.dom.Comment
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity(), LifecycleOwner {

    private val REQUEST_CODE_PERMISSIONS = 10

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    var btnStage: Int = 0

    lateinit var mainBtn: Button
    lateinit var cameraLayout:View
    lateinit var phoneNumber:EditText

    var path: String = ""

    private val picture = false

    val database: FirebaseDatabase = FirebaseDatabase.getInstance();


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        viewFinder = findViewById(R.id.view_finder)

        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

         mainBtn = findViewById<Button>(R.id.main_btn)

        cameraLayout = findViewById<View>(R.id.capture_image_layout)

        phoneNumber = findViewById<EditText>(R.id.phone_number)



        mainBtn.setOnClickListener {

            if(btnStage == 0){
                cameraLayout.visibility = View.VISIBLE
                btnStage = 1
                mainBtn.visibility = View.GONE
            }else if (btnStage==1){

            }
        }


    }

    private fun checkNumber(){
        val myRef = database.getReference("profile")

        if(phoneNumber.text == null || phoneNumber.text.toString().length != 10){
            mainBtn.visibility = View.VISIBLE
            Snackbar.make(findViewById(android.R.id.content), "Number Incorrect",
                Snackbar.LENGTH_SHORT).show()
            return
        }

        val numberQuery = database.getReference("profile").child(phoneNumber.text.toString())

        myRef.orderByChild("number").equalTo(phoneNumber.text.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.exists()){
                        dataSnapshot.children.forEach {
                            //var sp = it.getValue(Comment::class.java)
                            Toast.makeText(applicationContext,"exist", Toast.LENGTH_SHORT).show()
                           // var count = dataSnapshot.getValue(User.class)
                            //count = count + 1
                            //dataSnapshot.getRef().child("visit_count").setValue(count)
                            var user =it.getValue() as MutableMap<String, Any>
                            var count = user.get("visit_count").toString().toInt() + 1
                            it.ref.child("visit_count").setValue(count)





                        }
                    }else{
                        val user = User(phoneNumber.text.toString(), 1)


                        database.reference.child("profile").push().setValue(user)

                    }
                }
                override fun onCancelled(p0: DatabaseError) {

                }
            })





    }


    private lateinit var viewFinder: TextureView

    private fun startCamera() {

        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.FRONT)

            setTargetAspectRatio(Rational(1, 1))
            setTargetResolution(Size(640, 640))
        }.build()

        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {

            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setLensFacing(CameraX.LensFacing.FRONT)

                setTargetAspectRatio(Rational(1, 1))

                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
            val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(file,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(error: ImageCapture.UseCaseError,
                                         message: String, exc: Throwable?) {
                        val msg = "Photo capture failed: $message"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.e("CameraXApp", msg)
                        exc?.printStackTrace()
                    }

                    override fun onImageSaved(file: File) {
                        val msg = "Photo capture succeeded: ${file.path}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                        val straem = File(file.absolutePath)

                        val compressedImgFile =  Compressor(applicationContext).setMaxWidth(640)
                            .setMaxHeight(480)
                            .setCompressFormat(Bitmap.CompressFormat.WEBP)
                            .setDestinationDirectoryPath(file.parent+"/compressed")
                            .setQuality(75).compressToFile(straem)
                        path = compressedImgFile.path
                        cameraLayout.visibility = View.GONE

                        checkNumber()

                        Log.d("CameraXApp", msg)
                    }
                })
        }



        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {

        val matrix = Matrix()

        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        viewFinder.setTransform(matrix)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
