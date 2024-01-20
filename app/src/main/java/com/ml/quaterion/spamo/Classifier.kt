package com.ml.quaterion.spamo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.util.*
import kotlin.collections.HashMap

class Classifier(
    private val context: Context
) {

    private lateinit var vocabData : HashMap<String,Int>
    private lateinit var tfLiteInterpreter : Interpreter

    fun load(
        modelAssetsName: String ,
        vocabAssetsName: String ,
        onComplete: () -> Unit
    ) {
        CoroutineScope( Dispatchers.Default ).launch {
            val interpreter = loadModel( modelAssetsName )
            val vocab = loadVocab( vocabAssetsName )
            if( vocab != null && interpreter != null ) {
                this@Classifier.vocabData = vocab
                this@Classifier.tfLiteInterpreter = interpreter
                withContext( Dispatchers.Main ) {
                    onComplete()
                }
            }
            else {
                throw Exception( "Could not load model" )
            }
        }
    }


    fun classify(
        text: String ,
        onComplete: ((FloatArray) -> Unit)
    ) {
        CoroutineScope( Dispatchers.Default ).launch {
            val inputs : Array<FloatArray> = arrayOf(
                padSequence( tokenize( text ) )
                .map{ it.toFloat() }
                .toFloatArray()
            )
            // Output shape -> ( 1 , 2 ) ( as numClasses = 2 )
            val outputs : Array<FloatArray> = arrayOf( FloatArray( 2 ) )
            tfLiteInterpreter.run( inputs , outputs )
            onComplete( outputs[0] )
        }
    }

    // Tokenize the given sentence
    fun tokenize(
        message : String
    ): IntArray {
        return message
            .split(" " )
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { part -> vocabData[part] ?: 0 }
            .toIntArray()
    }

    // Pad the given sequence to `maxlen` with zeros.
    fun padSequence(
        sequence : IntArray
    ) : IntArray {
        val paddedSequence = IntArray( 120 ){ 0 }
        sequence.forEachIndexed { index, i ->
            paddedSequence[i] = index
        }
        return paddedSequence
    }

    private suspend fun loadVocab(
        vocabAssetsName: String
    ): HashMap<String,Int>? = withContext( Dispatchers.IO ) {
        Log.d( "Model" , "Loading vocab from $vocabAssetsName" )
        val inputStream = context.assets?.open( vocabAssetsName )
        if( inputStream != null ) {
            val reader = BufferedReader( InputStreamReader( inputStream ) )
            val jsonContents = reader.readText()
            val jsonObject = JSONObject( jsonContents )
            val iterator: Iterator<String> = jsonObject.keys()
            val data = HashMap<String, Int>()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val index = jsonObject.get( key )
                if( index is Int ) {
                    data[ key ] = index.toInt()
                }
            }
            return@withContext data
        }
        else { null }
    }

    private suspend fun loadModel(
        modelAssetsName: String
    ): Interpreter? = withContext( Dispatchers.IO ) {
        Log.d( "Model" , "Loading model from $modelAssetsName" )
        return@withContext try {
            Interpreter( FileUtil.loadMappedFile(context, modelAssetsName) )
        }
        catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

}