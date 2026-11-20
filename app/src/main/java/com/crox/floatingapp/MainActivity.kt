package com.crox.floatingapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crox.floatingapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_OVERLAY = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        updatePermissionStatus()

        binding.btnPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY)
            } else {
                Toast.makeText(this, "Izin sudah aktif!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnTampilkan.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Aktifkan izin overlay dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startFloatingService()
            Toast.makeText(this, "Bubble aktif!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnSembunyikan.setOnClickListener { stopFloatingService() }

        binding.rgShape.setOnCheckedChangeListener { _, checkedId ->
            val shape = when (checkedId) {
                R.id.rbKotak -> BubbleShape.SQUARE
                R.id.rbBulat -> BubbleShape.CIRCLE
                R.id.rbSegitiga -> BubbleShape.TRIANGLE
                else -> BubbleShape.CIRCLE
            }
            FloatingService.updateShape(shape)
        }

        binding.btnColorBlue.setOnClickListener { FloatingService.updateColor(BubbleColor.BLUE) }
        binding.btnColorGreen.setOnClickListener { FloatingService.updateColor(BubbleColor.GREEN) }
        binding.btnColorRed.setOnClickListener { FloatingService.updateColor(BubbleColor.RED) }
    }

    private fun updatePermissionStatus() {
        val ok = Settings.canDrawOverlays(this)
        binding.tvStatus.text = if (ok) "βœ… Aktif" else "❌ Belum diizinkan"
        binding.btnPermission.isEnabled = !ok
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingService::class.java).apply { action = FloatingService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun stopFloatingService() {
        startService(Intent(this, FloatingService::class.java).apply { action = FloatingService.ACTION_STOP })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) updatePermissionStatus()
    }

    override fun onResume() { super.onResume(); updatePermissionStatus() }
}
