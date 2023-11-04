package com.app.lifeguardians

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.app.lifeguardians.databinding.ActivityLoginBinding
import com.app.lifeguardians.databinding.ActivityMainBinding

class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.SignIn.setOnClickListener {
            finish()
        }

    }
    override fun onBackPressed() {

    }
}