package com.example.eternalvoicerecorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.CompoundButton
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var recording_time=60
    private var restore_files=5
    private var savedFileList=mutableListOf<String>()

    private var recorder: MediaRecorder? = null
    private var timerTask:TimerTask?=null

    private val TAG="eternalVoiceRecorder"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionCheck()
        switchCtrlInit()
        editTextCheck()

    }

    @SuppressLint("SimpleDateFormat")
    private fun makeFileName():String{
        var fileName=""
        val saveDirectory=File(editText_save.text.toString())

        if(!saveDirectory.exists()){
            if(!saveDirectory.mkdir())
                showToast("directory error!")
        }
        else {
            val currentTime = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm")
            fileName = saveDirectory.toString() +'/'+ dateFormat.format(currentTime) + ".m4a"
        }
        return fileName
    }

    private fun startRecording(){
        Log.d(TAG,"startRecording")
        val fileName=makeFileName()
        if(fileName.isNotEmpty()) {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(fileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e(TAG, "prepare() failed")
                }

                start()
                savedFileList.add(fileName)
            }
        }
    }
    private fun stopRecording(){
        Log.d(TAG,"stopRecording")
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        updateLogAndSaveDirectory()
    }
    private fun updateLogAndSaveDirectory(){
        var log="saved file:\n"
        editTextCheck()
        while(savedFileList.size>restore_files){
            val oldestFile=File(savedFileList[0])//latest index is beggest
            if(oldestFile.exists())oldestFile.delete()
            savedFileList.removeAt(0)
        }
        for(savedFile in savedFileList){
            log+=savedFile+"\n"
        }
        textView_log.text=log
    }

    private fun showToast(message:String){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }

    private fun switchCtrlInit(){
        switch_record.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            if(b){
                editTextCheck()
                savedFileList.clear()
                timerTask=kotlin.concurrent.timerTask {
                    runOnUiThread {
                        stopRecording()
                        startRecording()
                    }
                }
                Timer().scheduleAtFixedRate(timerTask,0,1000*60*recording_time.toLong())
            }
            else {
                timerTask?.cancel()
                timerTask=null
                stopRecording()
            }
        }
    }

    private fun editTextCheck(){
        recording_time=Integer.parseInt(editText_time.text.toString())
        restore_files=Integer.parseInt(editText_store.text.toString())
    }

    private fun permissionCheck(){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                ,1)

        }
    }
}

