package com.atsera.testapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
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
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.atsera.testapp.Login.Login
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseError
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File
import id.zelory.compressor.Compressor
import io.reactivex.internal.util.HalfSerializer.onComplete
import org.w3c.dom.Comment
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), LifecycleOwner {

    private val REQUEST_CODE_PERMISSIONS = 10

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    var btnStage: Int = 0

    lateinit var mainBtn: Button
    lateinit var cameraLayout:View
    lateinit var phoneNumber:EditText
    lateinit var parentView:View
    lateinit var getOTP: Button
    lateinit var otp:EditText

    lateinit var user:User;



    var path: String = ""

    private val picture = false

    val database: FirebaseDatabase = FirebaseDatabase.getInstance();


    //login
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private  val TAG: String = "login: "
    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    private var firebaseStore: FirebaseStorage? = null
    private var storageReference: StorageReference? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseStore = FirebaseStorage.getInstance()
        storageReference = FirebaseStorage.getInstance().reference


        parentView = findViewById(android.R.id.content) as View



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

        getOTP = findViewById<Button>(R.id.senotp)

        otp = findViewById<EditText>(R.id.login_otp)





        var callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$credential")

                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {

                Log.w(TAG, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Log.d(TAG, "Invalid phone number")
                    Snackbar.make(parentView, "Invalid phone number.",
                        Snackbar.LENGTH_SHORT).show()
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Log.d(TAG, "SMS quota exceeded contact support")
                    Snackbar.make(parentView, "SMS quota exceeded contact support",
                        Snackbar.LENGTH_SHORT).show()


                }

                // Show a message and update the UI


            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them latevr
                storedVerificationId = verificationId
                resendToken = token

                Snackbar.make(findViewById(android.R.id.content), "Code sent!",
                    Snackbar.LENGTH_SHORT).show()
            }
        }

        getOTP.setOnClickListener{
            otp.visibility = View.VISIBLE


                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    "+91"+phoneNumber.text.toString(),
                    30,
                    TimeUnit.SECONDS,
                    this@MainActivity,
                    callbacks)

        }




        mainBtn.setOnClickListener {

            val imm = it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)

            if(btnStage == 0){
                cameraLayout.visibility = View.VISIBLE
                btnStage = 1
                mainBtn.visibility = View.GONE
            }else if (btnStage==1){
                checkNumber()
            }else if(btnStage == 2){
                verifyPhoneNumberWithCode(storedVerificationId, otp.text.toString())

            }
        }


    }



    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        // [START verify_with_code]
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.d(TAG, "signInWithCredential:success")

                    database.reference.child("profile").push().setValue(user)


                        val uripath = Uri.parse("file://"+path)

                        val ref = storageReference?.child("uploads/" + UUID.randomUUID().toString())
                        val uploadTask = ref?.putFile(uripath)

                        val urlTask = uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                            if (!task.isSuccessful) {
                                task.exception?.let {
                                    throw it
                                }
                            }
                            return@Continuation ref.downloadUrl
                        })?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val downloadUri = task.result

                                var s = task.result?.encodedPath.toString()
                                val myRef = database.getReference("profile")

                                myRef.orderByChild("number").equalTo(phoneNumber.text.toString())
                                    .addListenerForSingleValueEvent(object : ValueEventListener{
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            if(dataSnapshot.exists()){
                                                dataSnapshot.children.forEach {

                                                    it.ref.child("url").setValue(s)

                                                    val intent = Intent(this@MainActivity, Login::class.java)
                                                    var user =it.getValue() as MutableMap<String, Any>
                                                    var count = user.get("visit_count").toString().toInt()
                                                    intent.putExtra("count",count)
                                                    startActivity(intent)

                                                }
                                            }
                                        }
                                        override fun onCancelled(p0: DatabaseError) {

                                        }
                                    })
                                Snackbar.make(parentView, "Upload Completed",
                                    Snackbar.LENGTH_SHORT).show()
                            } else {
                                // Handle failures
                                Snackbar.make(parentView, "Upload Failed try again",
                                    Snackbar.LENGTH_SHORT).show()
                            }
                        }?.addOnFailureListener{

                        }



                    val user = task.result?.user
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Snackbar.make(parentView, "Wrong code",
                            Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
    }



    private fun checkNumber(){
        val myRef = database.getReference("profile")

        if(phoneNumber.text == null || phoneNumber.text.toString().length != 10){
            mainBtn.visibility = View.VISIBLE
            mainBtn.text = "Sumbit"

            Snackbar.make(parentView,"Number Incorrect", Snackbar.LENGTH_LONG).show()
            return
        }


        myRef.orderByChild("number").equalTo(phoneNumber.text.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.exists()){
                        dataSnapshot.children.forEach {
                            Toast.makeText(applicationContext,"exist", Toast.LENGTH_SHORT).show()

                            var user =it.getValue() as MutableMap<String, Any>
                            var count = user.get("visit_count").toString().toInt() + 1
                            it.ref.child("visit_count").setValue(count)

                            val intent = Intent(this@MainActivity, Login::class.java)

                            intent.putExtra("count",count)
                            startActivity(intent)

                        }
                    }else{

                        phoneNumber.isEnabled = false;
                        mainBtn.visibility = View.VISIBLE
                        mainBtn.text = "Sumbit"
                        btnStage = 2

                        getOTP.performClick()

                        user = User(phoneNumber.text.toString(), 1, "")

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
                        //Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

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


    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth.currentUser
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
