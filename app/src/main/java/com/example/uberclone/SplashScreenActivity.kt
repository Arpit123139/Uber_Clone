package com.example.uberclone

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.uberclone.Model.DriverInfoModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object{
        private var LOGIN_REQUEST_CODE=7171
    }
    private lateinit var providers:List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener:FirebaseAuth.AuthStateListener
    private lateinit var database:FirebaseDatabase
    private lateinit var driverInfo:DatabaseReference                              // A Firebase reference represents a particular location in your Database and can be used for reading or writing data to that Database location.
   // This class is the starting point for all Database operations. After you've initialized it with a URL, you can use it to read data, write data, and to create new DatabaseReferences

    override fun onStart() {
        super.onStart()
        displaySplashScreen()
    }

    override fun onStop() {
        if(firebaseAuth!=null && listener!=null)
            firebaseAuth.removeAuthStateListener (listener )
        super.onStop()

    }
    private fun displaySplashScreen() {
        Completable.timer(3,TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe{
               firebaseAuth.addAuthStateListener(listener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        init()
    }

    private fun init(){

        database= FirebaseDatabase.getInstance();
        driverInfo=database.getReference()

        providers= Arrays.asList(

            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth=FirebaseAuth.getInstance()
        listener=FirebaseAuth.AuthStateListener { myFirebaseAuth->                   // Agar user phele se hi SignIn hai toh layout show nhi Karega
            val user=myFirebaseAuth.currentUser
            if(user!=null){
                checkUserFromFirebase()
            }
            else
                showLoginLayout()
         }
    }

    private fun checkUserFromFirebase() {
        driverInfo.child("DriverInfo").child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    if(snapshot.exists()){
                        Toast.makeText(this@SplashScreenActivity,"User Already Exsist",Toast.LENGTH_LONG).show()
                        val model=snapshot.getValue(DriverInfoModel::class.java)
                        findViewById<ProgressBar>(R.id.progress_bar).visibility=View.GONE
                        goToHomeActivity(model)
                    }else{
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity,error.message,Toast.LENGTH_LONG).show()

                }

            })
    }

    private fun goToHomeActivity(model: DriverInfoModel?) {

        Common.currentUser=model
        startActivity(Intent(this@SplashScreenActivity,DriverHomeActivity::class.java))
        finish()

    }

    @SuppressLint("MissingInflatedId")
    private fun showRegisterLayout() {

        val builder=AlertDialog.Builder(this,R.style.DialogTheme)
        val itemView=LayoutInflater.from(this).inflate(R.layout.layout_register,null)

        val firstName=itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val LastName=itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val PhoneNumber=itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText

        val btn_continue=itemView.findViewById<View>(R.id.btn_continue) as Button

        if(FirebaseAuth.getInstance().currentUser!!.phoneNumber!=null && !TextUtils.isEmpty(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            PhoneNumber.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        builder.setView(itemView)
        val dialog=builder.create()
        dialog.show()

            btn_continue.setOnClickListener {
                if(TextUtils.isEmpty(firstName.text.toString())){

                    Toast.makeText(this@SplashScreenActivity,"Plz Enter the First Name",Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                else if(TextUtils.isEmpty(LastName.text.toString())){
                    Toast.makeText(this@SplashScreenActivity,"Plz Enter the Last Name",Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                else if(TextUtils.isEmpty(PhoneNumber.text.toString())){
                    Toast.makeText(this@SplashScreenActivity,"Plz Enter the Phone Number",Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                else{
                    val model=DriverInfoModel(firstName.text.toString(),LastName.text.toString(),PhoneNumber.text.toString())
                    driverInfo.child("DriverInfo").child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(model)
                        .addOnFailureListener{e->

                            Toast.makeText(this@SplashScreenActivity,""+e.message,Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                            findViewById<ProgressBar>(R.id.progress_bar).visibility=View.GONE
                        }
                        .addOnSuccessListener {
                            Toast.makeText(this@SplashScreenActivity,"Driver Added Successfully",Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                            goToHomeActivity(model)
                            findViewById<ProgressBar>(R.id.progress_bar).visibility=View.GONE
                        }

                }
            }


    }

    private fun showLoginLayout() {
        val authMethodPickerLayour=AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()

        //It will except the result from the another Acivity
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayour)
                .setTheme(R.style.GreenTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
            , LOGIN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== LOGIN_REQUEST_CODE){

            val response =IdpResponse.fromResultIntent(data)
            if(resultCode== Activity.RESULT_OK){
                val user=FirebaseAuth.getInstance().currentUser
            }else{
                Toast.makeText(this,""+response!!.error!!.message,Toast.LENGTH_LONG).show()
            }
        }

    }


}