package com.notayan.wallwidgy.search

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class SearchEngine private constructor(private val embedder: TextEmbedder) {
    private val lock = Any()

    fun embed(text: String): FloatArray? {
        if (text.isBlank()) return null
        return synchronized(lock) {
            try {
                val result = embedder.embed(text)
                val embeddings = result.embeddingResult().embeddings()
                if (embeddings.isNotEmpty()) {
                    embeddings[0].floatEmbedding()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun close() {
        synchronized(lock) {
            try {
                embedder.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun create(context: Context, modelFile: File): SearchEngine? {
            return try {
                if (!modelFile.exists()) return null
                val buffer = loadModelFileAsBuffer(modelFile)
                val baseOptions = BaseOptions.builder()
                    .setModelAssetBuffer(buffer)
                    .build()
                val options = TextEmbedderOptions.builder()
                    .setBaseOptions(baseOptions)
                    .build()
                val embedder = TextEmbedder.createFromOptions(context, options)
                SearchEngine(embedder)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun loadModelFileAsBuffer(file: File): ByteBuffer {
            val inputStream = FileInputStream(file)
            val fileChannel = inputStream.channel
            val startOffset = 0L
            val declaredLength = fileChannel.size()
            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            inputStream.close()
            return buffer
        }

        fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
            if (vec1.size != vec2.size) return 0.0
            var dotProduct = 0.0
            var normA = 0.0
            var normB = 0.0
            for (i in vec1.indices) {
                dotProduct += vec1[i] * vec2[i]
                normA += vec1[i] * vec1[i]
                normB += vec2[i] * vec2[i]
            }
            return if (normA == 0.0 || normB == 0.0) 0.0 else dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))
        }
    }
}
