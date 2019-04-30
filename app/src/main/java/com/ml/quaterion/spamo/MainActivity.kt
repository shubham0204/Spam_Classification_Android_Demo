package com.ml.quaterion.spamo

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        classify_button.setOnClickListener( View.OnClickListener {
            val classifier = Classifier( this , "word_dict.json")
            classifier.setMaxLength( 171 )
            classifier.setCallback( object : Classifier.DataCallback {
                override fun onDataProcessed( result :HashMap<String, Int>? ) {
                    val message = message_text.text.toString().toLowerCase().trim()
                    if ( !TextUtils.isEmpty( message ) ){
                        classifier.setVocab( result )
                        val tokenizedMessage = classifier.tokenize( message )
                        val paddedMessage = classifier.padSequence( tokenizedMessage )
                        val results = classifySequence( paddedMessage )
                        val class1 = results[0]
                        val class2 = results[1]
                        result_text.text = "SPAM : $class2\nNOT SPAM : $class1 "
                    }
                    else{
                        Toast.makeText( this@MainActivity, "Please enter a message.", Toast.LENGTH_LONG).show();
                    }

                }
            })
            classifier.loadData()
        })

    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val MODEL_ASSETS_PATH = "model.tflite"
        val assetFileDescriptor = assets.openFd(MODEL_ASSETS_PATH)
        val fileInputStream = FileInputStream(assetFileDescriptor.getFileDescriptor())
        val fileChannel = fileInputStream.getChannel()
        val startoffset = assetFileDescriptor.getStartOffset()
        val declaredLength = assetFileDescriptor.getDeclaredLength()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength)
    }

    fun classifySequence ( sequence : IntArray ): FloatArray {
        val interpreter = Interpreter( loadModelFile() )
        val inputs : Array<FloatArray> = arrayOf( sequence.map{ it.toFloat() }.toFloatArray() )
        val outputs : Array<FloatArray> = arrayOf( floatArrayOf( 0.0f , 0.0f ) )
        interpreter.run( inputs , outputs )
        return outputs[0]
    }

}
