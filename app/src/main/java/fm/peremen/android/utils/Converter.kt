package fm.peremen.android.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.yield
import timber.log.Timber

private const val TIMEOUT = -1L

suspend fun convertMp3ToPcm(context: Context, inputFilename: String, outputFilename: String): MediaFormat {
    val inputFile = context.assets.openFd(inputFilename)
    val outputStream = context.openFileOutput(outputFilename, MODE_PRIVATE)

    val extractor = MediaExtractor()
    extractor.setDataSource(inputFile)
    extractor.selectTrack(0)

    val inputFormat = extractor.getTrackFormat(0)
    Timber.d("inputFormat: $inputFormat")

    val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
    val codec = MediaCodec.createDecoderByType(mime)
    try {
        codec.configure(inputFormat, null, null, 0);

        val outputFormat = codec.outputFormat
        Timber.d("outputFormat: $outputFormat")

        codec.start()

        var inputEos = false;
        var outputEos = false;
        val info = MediaCodec.BufferInfo()
        var inputSize = 0

        while (!outputEos) {
            yield()

            if (!inputEos) {
                val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                    var sampleSize = extractor.readSampleData(inputBuffer, 0)
//                    Timber.d("sampleSize: $sampleSize")

                    inputSize += sampleSize
//                    Timber.d("inputSize: $inputSize")

                    var presentationTimeUs: Long = 0
                    if (sampleSize < 0) {
                        Timber.d("inputEos eos.")
                        inputEos = true
                        sampleSize = 0
                    } else {
                        presentationTimeUs = extractor.sampleTime
                    }

                    codec.queueInputBuffer(
                        inputBufferIndex, 0, sampleSize, presentationTimeUs,
                        if (inputEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    )
                    if (!inputEos) {
                        extractor.advance()
                    }
                }
            }

            val res = codec.dequeueOutputBuffer(info, TIMEOUT);
//            Timber.d("info.size: ${info.size}")

            if (res >= 0) {
                val outputBufIndex = res
                val buf = codec.getOutputBuffer(outputBufIndex)!!
                val dst = ByteArray(info.size)
                val oldPosition = buf.position()
                buf.get(dst);
                buf.position(oldPosition)

                outputStream.write(dst)

                codec.releaseOutputBuffer(outputBufIndex, false);

                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Timber.d("output eos.")
                    outputEos = true
                }
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Timber.d("output format changed: ${codec.outputFormat}");
            }
        }

        return outputFormat
    } finally {
        codec.stop();
        codec.release();
        outputStream.close()
    }
}