package com.ml.quaterion.spamo

import android.app.ProgressDialog
import android.os.Bundle
import android.os.PersistableBundle
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.ml.quaterion.spamo.ui.theme.AppTheme
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class MainActivity : ComponentActivity() {

    // Name of TFLite model ( in /assets folder ).
    private val MODEL_ASSETS_PATH = "model.tflite"

    // Max Length of input sequence. The input shape for the model will be ( None , INPUT_MAXLEN ).
    private val INPUT_MAXLEN = 171

    private lateinit var tfLiteInterpreter : Interpreter
    private lateinit var classifier: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ActivityUI()
        }

        // Init the classifier.
        classifier = Classifier( this , "word_dict.json" , INPUT_MAXLEN )
        // Init TFLiteInterpreter
        tfLiteInterpreter = Interpreter( loadModelFile() )

        // Start vocab processing, show a ProgressDialog to the user.
        val progressDialog = ProgressDialog( this )
        progressDialog.setMessage( "Parsing word_dict.json ..." )
        progressDialog.setCancelable( false )
        progressDialog.show()
        classifier.processVocab( object: Classifier.VocabCallback {
            override fun onVocabProcessed() {
                // Processing done, dismiss the progressDialog.
                progressDialog.dismiss()
            }
        })


    }

    @Composable
    private fun ActivityUI() {
        AppTheme {
            Surface(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxSize() ,
            ) {
                MessageInput()
            }
        }
    }

    @Composable
    private fun MessageInput() {
        var message by remember{ mutableStateOf( "" ) }
        var resultText by remember{ mutableStateOf( "" ) }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth() ,
                value = message,
                onValueChange = { message = it } )
            Button(onClick = {
                if ( !TextUtils.isEmpty( message ) ){
                    // Tokenize and pad the given input text.
                    val tokenizedMessage = classifier.tokenize( message )
                    val paddedMessage = classifier.padSequence( tokenizedMessage )

                    val results = classifySequence( paddedMessage )
                    val class1 = results[0]
                    val class2 = results[1]
                    resultText = "SPAM : $class2\nNOT SPAM : $class1 "
                }
                else{
                    Toast.makeText( this@MainActivity, "Please enter a message.", Toast.LENGTH_LONG).show();
                }
            }) {
                Text(text = "Classify")
            }
            Text(text = resultText)
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd(MODEL_ASSETS_PATH)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Perform inference, given the input sequence.
    private fun classifySequence (sequence : IntArray ): FloatArray {
        // Input shape -> ( 1 , INPUT_MAXLEN )
        val inputs : Array<FloatArray> = arrayOf( sequence.map { it.toFloat() }.toFloatArray() )
        // Output shape -> ( 1 , 2 ) ( as numClasses = 2 )
        val outputs : Array<FloatArray> = arrayOf( FloatArray( 2 ) )
        tfLiteInterpreter?.run( inputs , outputs )
        return outputs[0]
    }

}
