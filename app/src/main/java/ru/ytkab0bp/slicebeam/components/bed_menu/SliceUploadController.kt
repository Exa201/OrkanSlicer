package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.PixelCopy
import androidx.appcompat.app.AlertDialog
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.entity.ContentType
import cz.msebera.android.httpclient.message.BasicHeader
import org.json.JSONObject
import ru.ytkab0bp.slicebeam.BuildConfig
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.utils.AnycubicGCodeProcessor
import ru.ytkab0bp.slicebeam.utils.AnycubicAceRoutingPolicy
import ru.ytkab0bp.slicebeam.utils.GCodeIntegrityValidator
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.utils.MoonrakerResponseInspector
import ru.ytkab0bp.slicebeam.utils.OctoPrintResponseInspector
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.PrinterResponseInspector
import ru.ytkab0bp.slicebeam.utils.PrinterUploadReservation
import ru.ytkab0bp.slicebeam.utils.PrinterUploadUrl
import ru.ytkab0bp.slicebeam.utils.QidiGCode3mfPackager
import ru.ytkab0bp.slicebeam.utils.UploadFileValidator
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Owns the complete post-slice file preparation, validation and printer upload flow. */
object SliceUploadController {
    /** One in-flight send/start per printer, including file preparation and upload. */
    private val activeUploadHosts = PrinterUploadReservation()
    private val uploadReservations = ConcurrentHashMap<ProgressDialog, String>()
    private val client = AsyncHttpClient().apply {
        setLoggingEnabled(true)
        // Moonraker on Kobra 3 can preprocess uploaded G-code before responding.
        setTimeout(300000)
        setConnectTimeout(15000)
        setMaxRetriesAndTimeout(0, 10000)
    }
    private val printStartClient = AsyncHttpClient().apply {
        setLoggingEnabled(true)
        // Uploads may take minutes; a print-start acknowledgement should not.
        setTimeout(30000)
        setConnectTimeout(15000)
        setMaxRetriesAndTimeout(0, 10000)
    }

    @JvmStatic
    fun upload(
        fragment: BedFragment,
        type: String,
        host: String,
        apiKey: String?,
        print: Boolean,
        printer: ConfigObject,
        activeTools: IntArray,
    ) {
        val context = fragment.getContext() ?: return
        val slicedResult = fragment.glView?.renderer?.gcodeResult
        if (!fragment.hasSliceResult() || slicedResult == null) {
            showPreparationFailure(fragment, context.getString(R.string.UploadSliceMissing))
            return
        }
        val reservation = activeUploadHosts.reserve(host)
        if (reservation == null) {
            showPreparationFailure(fragment, "Für diesen Drucker läuft bereits ein Upload oder Druckstart. Bitte warte, bis der Auftrag abgeschlossen ist.")
            return
        }
        val progress = ProgressDialog(context).apply {
            setTitle(R.string.MenuSliceSendToPrinterLoading)
            setMessage(context.getString(R.string.UploadPreparingGcode))
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = true
            setCancelable(false)
            show()
        }
        uploadReservations[progress] = reservation
        val name = slicedResult.getRecommendedName()
        val source = BedFragment.tempGCodePath
        val currentModel: Model? = fragment.currentModel
        val modelInstances = currentModel?.objectsCount?.coerceAtLeast(1) ?: 1
        val qidiQ2 = QidiGCode3mfPackager.isQidiQ2(printer)
        val anycubicKobra3 = isAnycubicKobra3(printer)
        val aceBefore = if (anycubicKobra3) MmuFilamentSync.assignedGates(
            printer.title.orEmpty(), activeTools) else null
        val uploadName = if (qidiQ2) QidiGCode3mfPackager.uploadName(name) else name
        val continuePreparation: (ByteArray?) -> Unit = { preview -> IOUtils.IO_POOL.execute {
            var uploadFile: File? = null
            var preparationError: Exception? = null
            var integrityFailure: GCodeIntegrityValidator.Result? = null
            val sourceCheck = GCodeIntegrityValidator.validate(source)
            if (!sourceCheck.isValid()) {
                integrityFailure = sourceCheck
            } else {
                try {
                    uploadFile = if (qidiQ2) prepareQidiUpload(source, printer, preview, uploadName)
                    else prepareAnycubicUpload(source, printer, modelInstances, fragment, preview)
                    if (uploadFile != null) {
                        if (qidiQ2) {
                            if (!QidiGCode3mfPackager.verify(uploadFile!!)) {
                                preparationError = IOException("The QIDI Q2 package failed its structure or checksum check.")
                                uploadFile = null
                            }
                        } else {
                            val preparedCheck = GCodeIntegrityValidator.validate(uploadFile!!)
                            if (!preparedCheck.isValid()) {
                                integrityFailure = preparedCheck
                                uploadFile = null
                            }
                        }
                    }
                } catch (error: Exception) {
                    preparationError = error
                    Log.e("SliceMenu", "Could not prepare printer file for upload", error)
                }
            }
            val preparedFile = uploadFile
            val failure = preparationError
            val invalidGcode = integrityFailure
            ViewUtils.postOnMainThread {
                if (fragment.getContext() == null) {
                    dismissSendProgress(progress)
                } else if (invalidGcode != null) {
                    dismissSendProgress(progress)
                    showGCodeIntegrityFailure(fragment, invalidGcode)
                } else if (failure != null) {
                    dismissSendProgress(progress)
                    showPreparationFailure(fragment, failure.message)
                } else if (preparedFile != null) {
                    progress.isIndeterminate = false
                    progress.progress = 0
                    progress.setMessage(fragment.getContext()!!.getString(R.string.UploadProgressPercent, 0))
                    uploadPrepared(fragment, type, host, apiKey, uploadName, print, preparedFile, progress, qidiQ2)
                } else {
                    dismissSendProgress(progress)
                }
            }
        } }
        val startFilePreparation = {
            if (qidiQ2 || anycubicKobra3) captureWorkspacePreview(fragment, continuePreparation)
            else continuePreparation(null)
        }
        if (anycubicKobra3) {
            progress.setMessage(context.getString(R.string.UploadAceChecking))
            MmuFilamentSync.synchronize(printer) { _, error ->
                if (fragment.getContext() == null) {
                    dismissSendProgress(progress)
                } else if (error != null) {
                    dismissSendProgress(progress)
                    showPreparationFailure(fragment, context.getString(R.string.UploadAceCheckFailed))
                } else if (aceBefore == null || aceBefore != MmuFilamentSync.assignedGates(
                        printer.title.orEmpty(), activeTools)) {
                    dismissSendProgress(progress)
                    showPreparationFailure(fragment, context.getString(R.string.UploadAceAssignmentChanged))
                } else {
                    progress.setMessage(context.getString(R.string.UploadPreparingGcode))
                    startFilePreparation()
                }
            }
        } else startFilePreparation()
    }

    @JvmStatic
    fun showPreparationFailure(fragment: BedFragment, detail: String? = null) {
        ViewUtils.postOnMainThread {
            val context = fragment.getContext() ?: return@postOnMainThread
            BeamAlertDialogBuilder(context)
                .setTitle(R.string.MenuSliceSendToPrinterFailed)
                .setMessage(detail?.takeIf { it.isNotBlank() } ?: context.getString(R.string.AnycubicGcodePrepareFailed))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun uploadPrepared(
        fragment: BedFragment,
        type: String,
        host: String,
        apiKey: String?,
        name: String,
        print: Boolean,
        uploadFile: File,
        progress: ProgressDialog,
        qidiQ2: Boolean,
    ) {
        if (!UploadFileValidator.isReadyForUpload(uploadFile)) {
            dismissSendProgress(progress)
            showPreparationFailure(fragment)
            return
        }
        if (type == "moonraker" || type == "klipper") {
            uploadMoonraker(fragment, host, apiKey, name, print, uploadFile, progress, qidiQ2)
            return
        }

        val uploadUrl = PrinterUploadUrl.endpoint(host, "/api/files/local")
        if (uploadUrl == null) {
            showUploadFailure(fragment, 0, null, IOException("Die Druckeradresse ist ungültig. Prüfe Hostname und Port."), progress)
            return
        }
        val headers: Array<Header> = if (TextUtils.isEmpty(apiKey)) emptyArray()
        else arrayOf(BasicHeader("X-Api-Key", apiKey))
        val params = RequestParams()
        try {
            params.put(
                "file", FileInputStream(uploadFile), name,
                if (qidiQ2) ContentType.APPLICATION_OCTET_STREAM.mimeType else ContentType.TEXT_PLAIN.mimeType,
            )
        } catch (error: IOException) {
            showUploadFailure(fragment, 0, null, error, progress)
            return
        } catch (error: RuntimeException) {
            showUploadFailure(fragment, 0, null, error, progress)
            return
        }
        params.put("select", print.toString())
        params.put("print", print.toString())

        try {
        client.post(
            SliceBeam.requireInstance(),
            uploadUrl,
            headers,
            params,
            null,
            object : AsyncHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<Header>, responseBody: ByteArray) {
                    PrinterResponseInspector.embeddedError(responseBody)?.let { detail ->
                        showUploadFailure(fragment, 0, responseBody, IOException(detail), progress)
                        return
                    }
                    val acknowledgement = OctoPrintResponseInspector.uploadedFile(responseBody)
                    if (acknowledgement == null) {
                        showUploadFailure(fragment, 0, null, IOException(
                            PrinterResponseInspector.missingAcknowledgement(responseBody,
                                SliceBeam.requireInstance().getString(R.string.UploadFileNotConfirmed))), progress)
                        return
                    }
                    dismissSendProgress(progress)
                    val outcome = acknowledgement.outcome(print)
                    val message = when (outcome) {
                        OctoPrintResponseInspector.UploadOutcome.PRINT_STARTED -> R.string.MenuSliceSendToPrinterPrintStarted
                        OctoPrintResponseInspector.UploadOutcome.SELECTED_NOT_STARTED -> R.string.UploadSelectedNotStarted
                        OctoPrintResponseInspector.UploadOutcome.PRINT_NOT_STARTED -> R.string.UploadPrintNotStarted
                        OctoPrintResponseInspector.UploadOutcome.PROCESSING -> R.string.UploadStillProcessing
                        OctoPrintResponseInspector.UploadOutcome.UPLOADED -> R.string.MenuSliceSendToPrinterOK
                    }
                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(
                        if (outcome == OctoPrintResponseInspector.UploadOutcome.PRINT_STARTED ||
                            outcome == OctoPrintResponseInspector.UploadOutcome.UPLOADED)
                            SnackbarsLayout.Type.DONE else SnackbarsLayout.Type.INFO,
                        message,
                    ))
                }

                override fun onProgress(bytesWritten: Long, totalSize: Long) {
                    updateSendProgress(fragment, progress, bytesWritten, totalSize)
                }

                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header>,
                    responseBody: ByteArray,
                    error: Throwable,
                ) {
                    showUploadFailure(fragment, statusCode, responseBody, error, progress)
                }
            },
        )
        } catch (error: RuntimeException) {
            showUploadFailure(fragment, 0, null, error, progress)
        }
    }

    private fun uploadMoonraker(
        fragment: BedFragment,
        host: String,
        apiKey: String?,
        name: String,
        print: Boolean,
        uploadFile: File,
        progress: ProgressDialog,
        qidiQ2: Boolean,
    ) {
        val uploadUrl = PrinterUploadUrl.endpoint(host, "/server/files/upload")
        val startUrl = PrinterUploadUrl.endpoint(host, "/printer/print/start")
        if (uploadUrl == null || startUrl == null) {
            showUploadFailure(fragment, 0, null, IOException("Die Druckeradresse ist ungültig. Prüfe Hostname und Port."), progress)
            return
        }
        val headers: Array<Header> = if (TextUtils.isEmpty(apiKey)) emptyArray()
        else arrayOf(BasicHeader("X-Api-Key", apiKey))
        val params = RequestParams()
        try {
            params.put(
                "file", FileInputStream(uploadFile), name,
                if (qidiQ2) ContentType.APPLICATION_OCTET_STREAM.mimeType else ContentType.TEXT_PLAIN.mimeType,
            )
        } catch (error: IOException) {
            showUploadFailure(fragment, 0, null, error, progress)
            return
        } catch (error: RuntimeException) {
            showUploadFailure(fragment, 0, null, error, progress)
            return
        }
        params.put("root", "gcodes")
        params.put("path", "")
        try {
        client.post(
            SliceBeam.requireInstance(),
            uploadUrl,
            headers,
            params,
            null,
            object : AsyncHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, responseHeaders: Array<Header>, responseBody: ByteArray) {
                    PrinterResponseInspector.embeddedError(responseBody)?.let { detail ->
                        showUploadFailure(fragment, 0, responseBody, IOException(detail), progress)
                        return
                    }
                    val acknowledgement = MoonrakerResponseInspector.uploadedFile(responseBody, name)
                    if (acknowledgement == null) {
                        showUploadFailure(fragment, 0, null,
                            IOException(PrinterResponseInspector.missingAcknowledgement(responseBody,
                                "Der Drucker hat den Datei-Upload nicht bestätigt. Es wurde kein Druck gestartet.")),
                            progress)
                        return
                    }
                    if (acknowledgement.printStarted || acknowledgement.printQueued) {
                        dismissSendProgress(progress)
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(
                            SnackbarsLayout.Type.INFO,
                            if (acknowledgement.printStarted) R.string.UploadPrinterAlreadyStarted
                            else R.string.UploadPrinterQueued,
                        ))
                        return
                    }
                    if (!print) {
                        dismissSendProgress(progress)
                        SliceBeam.EVENT_BUS.fireEvent(
                            NeedSnackbarEvent(SnackbarsLayout.Type.DONE, R.string.MenuSliceSendToPrinterOK),
                        )
                        return
                    }
                    ViewUtils.postOnMainThread {
                        progress.setMessage(SliceBeam.requireInstance().getString(R.string.UploadPrinterProcessing))
                    }
                    val startParams = RequestParams().apply {
                        put("filename", acknowledgement.path)
                        if (qidiQ2) put("plateindex", "1")
                    }
                    try {
                    printStartClient.post(
                        SliceBeam.requireInstance(),
                        startUrl,
                        headers,
                        startParams,
                        null,
                        object : AsyncHttpResponseHandler() {
                            override fun onSuccess(code: Int, responseHeaders: Array<Header>, body: ByteArray) {
                                PrinterResponseInspector.embeddedError(body)?.let { detail ->
                                    showUploadFailure(fragment, 0, body, IOException(detail), progress, printStartAttempt = true)
                                    return
                                }
                                if (!MoonrakerResponseInspector.printStartConfirmed(body)) {
                                    checkUncertainPrintStart(fragment, host, headers, acknowledgement.path, progress,
                                        "Der Drucker hat den Druckstart nicht eindeutig bestätigt.")
                                    return
                                }
                                dismissSendProgress(progress)
                                SliceBeam.EVENT_BUS.fireEvent(
                                    NeedSnackbarEvent(
                                        SnackbarsLayout.Type.INFO,
                                        R.string.MenuSliceSendToPrinterPrintStarted,
                                    ),
                                )
                            }

                            override fun onFailure(
                                code: Int,
                                responseHeaders: Array<Header>,
                                body: ByteArray,
                                error: Throwable,
                            ) {
                                checkUncertainPrintStart(fragment, host, headers, acknowledgement.path, progress,
                                    "Die Antwort auf den Druckstart war unklar${if (code > 0) " (HTTP $code)" else ""}: " +
                                        PrinterResponseInspector.printStartFailureDetail(body, error.localizedMessage ?: error.toString()))
                            }
                        },
                    )
                    } catch (error: RuntimeException) {
                        checkUncertainPrintStart(fragment, host, headers, acknowledgement.path, progress,
                            "Die Startanfrage konnte nicht eindeutig abgeschlossen werden: ${error.localizedMessage ?: error}.")
                    }
                }

                override fun onProgress(bytesWritten: Long, totalSize: Long) {
                    updateSendProgress(fragment, progress, bytesWritten, totalSize)
                }

                override fun onFailure(
                    statusCode: Int,
                    responseHeaders: Array<Header>,
                    responseBody: ByteArray,
                    error: Throwable,
                ) {
                    showUploadFailure(fragment, statusCode, responseBody, error, progress)
                }
            },
        )
        } catch (error: RuntimeException) {
            showUploadFailure(fragment, 0, null, error, progress)
        }
    }

    private fun updateSendProgress(
        fragment: BedFragment,
        progress: ProgressDialog,
        written: Long,
        total: Long,
    ) {
        if (total <= 0L) return
        val percent = (written * 100L / total).coerceIn(0L, 100L).toInt()
        ViewUtils.postOnMainThread {
            if (!progress.isShowing) return@postOnMainThread
            progress.isIndeterminate = false
            progress.progress = percent
            val context = fragment.getContext()
            progress.setMessage(
                if (context == null) ""
                else context.getString(
                    if (percent >= 100) R.string.UploadPrinterProcessing else R.string.UploadProgressPercent,
                    percent,
                ),
            )
        }
    }

    private fun dismissSendProgress(progress: ProgressDialog) {
        uploadReservations.remove(progress)?.let(activeUploadHosts::release)
        ViewUtils.postOnMainThread {
            if (progress.isShowing) progress.dismiss()
        }
    }

    /** A start request can reach Klipper even when its HTTP response is lost; query once, never retry. */
    private fun checkUncertainPrintStart(
        fragment: BedFragment,
        host: String,
        headers: Array<Header>,
        uploadedPath: String,
        progress: ProgressDialog,
        reason: String,
    ) {
        val statusUrl = PrinterUploadUrl.endpoint(host, "/printer/objects/query?print_stats")
        if (statusUrl == null) {
            showUploadFailure(fragment, 0, null, IOException("$reason Druckerstatus konnte nicht abgefragt werden."), progress,
                printStartAttempt = true)
            return
        }
        ViewUtils.postOnMainThread {
            if (progress.isShowing) progress.setMessage("Druckstatus wird einmalig geprüft …")
        }
        val statusClient = AsyncHttpClient().apply {
            setTimeout(10_000)
            setConnectTimeout(5_000)
            setMaxRetriesAndTimeout(0, 5_000)
        }
        try {
            statusClient.get(SliceBeam.requireInstance(), statusUrl, headers, null,
                object : AsyncHttpResponseHandler() {
                    override fun onSuccess(code: Int, responseHeaders: Array<Header>, body: ByteArray) {
                        val status = MoonrakerResponseInspector.printStatus(body)
                        if (status?.confirmsStartedFile(uploadedPath) == true) {
                            dismissSendProgress(progress)
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(
                                SnackbarsLayout.Type.INFO, R.string.UploadPrinterFileActive,
                            ))
                        } else {
                            val observed = status?.let { " Aktueller Status: ${it.state}" }
                                ?: " Der Druckerstatus konnte nicht gelesen werden."
                            showUploadFailure(fragment, 0, null,
                                IOException("$reason$observed Es wurde kein weiterer Startbefehl gesendet; prüfe den Drucker, bevor du erneut startest."),
                                progress, printStartAttempt = true)
                        }
                    }

                    override fun onFailure(code: Int, responseHeaders: Array<Header>, body: ByteArray, error: Throwable) {
                        val detail = error.localizedMessage?.takeIf { it.isNotBlank() } ?: "HTTP $code"
                        showUploadFailure(fragment, code, body,
                            IOException("$reason Die Statusabfrage ist fehlgeschlagen ($detail). Es wurde kein weiterer Startbefehl gesendet; prüfe den Drucker, bevor du erneut startest."),
                            progress, printStartAttempt = true)
                    }
                })
        } catch (error: RuntimeException) {
            showUploadFailure(fragment, 0, null,
                IOException("$reason Die Statusabfrage konnte nicht ausgeführt werden. Es wurde kein weiterer Startbefehl gesendet.", error),
                progress, printStartAttempt = true)
        }
    }

    private fun showUploadFailure(
        fragment: BedFragment,
        statusCode: Int,
        responseBody: ByteArray?,
        error: Throwable?,
        progress: ProgressDialog,
        printStartAttempt: Boolean = false,
    ) {
        dismissSendProgress(progress)
        val detail = if (printStartAttempt)
            PrinterResponseInspector.printStartFailureDetail(responseBody, error?.localizedMessage ?: error?.toString())
        else PrinterResponseInspector.failureDetail(responseBody, error?.localizedMessage ?: error?.toString())
        ViewUtils.postOnMainThread {
            val context = fragment.getContext() ?: return@postOnMainThread
            BeamAlertDialogBuilder(context)
                .setTitle(R.string.MenuSliceSendToPrinterFailed)
                .setMessage((if (statusCode > 0) "HTTP $statusCode: " else "") +
                    detail.ifBlank { context.getString(R.string.MenuSliceSendToPrinterFailed) })
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    /** Anycubic Kobra 3 firmware requires vendor metadata in its G-code upload. */
    private fun prepareAnycubicUpload(
        source: File,
        printer: ConfigObject,
        modelInstances: Int,
        fragment: BedFragment,
        previewPng: ByteArray?,
    ): File? {
        if (!isAnycubicKobra3(printer)) return source

        val parent = source.parentFile ?: return null
        val prepared = File(parent, "anycubic_${source.name}")
        val scan = try {
            AnycubicGCodeProcessor.scan(source)
        } catch (error: IOException) {
            Log.e("SliceMenu", "Could not scan G-code before Anycubic upload", error)
            showPreparationFailure(fragment)
            return null
        }
        val logicalToolCount = maxOf(1, scan.maxTool + 1)
        val printerTitle = printer.title.orEmpty()
        val usedTools = try {
            SlicePreparationTools.readToolUsage(source).used
        } catch (error: IOException) {
            Log.e("SliceMenu", "Could not inspect ACE tool usage before upload", error)
            showPreparationFailure(fragment, error.message)
            return null
        }
        val loadedGates = MmuFilamentSync.loadedGates(printerTitle)
        val routing = AnycubicAceRoutingPolicy.resolve(
            logicalToolCount,
            usedTools,
            loadedGates,
            IntArray(logicalToolCount) { tool -> MmuFilamentSync.defaultGateForTool(printerTitle, tool) },
        )
        val unavailableTool = routing.unavailableTool
        if (unavailableTool != null) {
            showPreparationFailure(fragment, SliceBeam.requireInstance().getString(
                R.string.MmuDirectSlotUnavailable, unavailableTool, unavailableTool + 1,
            ))
            return null
        }
        val gates = routing.gates
        val remappedTools = routing.toolCommands

        val liveSlots = Prefs.getFilamentSlots()
        // Vendor metadata describes the ACE gate that receives the tool, not the
        // source 3MF palette. Keep slicer temperatures unchanged in the G-code.
        val physicalSlots = (0 until logicalToolCount).map { tool ->
            MmuFilamentSync.gateSlot(printerTitle, gates[tool])
                ?: (if (gates[tool] in loadedGates) liveSlots.getOrNull(tool) else null)
                ?: ru.ytkab0bp.slicebeam.utils.FilamentSlot(Color.rgb(25, 200, 160), "PLA")
        }
        val emittedPaintInfo = BooleanArray(4)
        var hasPaintInfo = false
        val paintInfo = buildString {
            append('[')
            for (tool in 0 until logicalToolCount) {
                val mappedTool = remappedTools[tool]
                if (mappedTool !in 0..3 || emittedPaintInfo[mappedTool]) continue
                emittedPaintInfo[mappedTool] = true
                if (hasPaintInfo) append(',')
                hasPaintInfo = true
                val slot = MmuFilamentSync.gateSlot(printerTitle, gates[tool])
                    ?: (if (gates[tool] in loadedGates) liveSlots.getOrNull(tool) else null)
                val material = slot?.type ?: scan.materials[minOf(tool, scan.materials.lastIndex)].trim()
                val paintColor = slot?.color ?: Color.rgb(25, 200, 160)
                append("{\"material_type\":").append(JSONObject.quote(material))
                    .append(",\"paint_color\":[").append(Color.red(paintColor)).append(',')
                    .append(Color.green(paintColor)).append(',').append(Color.blue(paintColor))
                    .append("],\"paint_index\":").append(mappedTool).append('}')
            }
            append(']')
        }
        val options = AnycubicGCodeProcessor.Options().apply {
            this.remappedTools = remappedTools
            this.gates = gates
            this.modelInstances = modelInstances
            versionName = BuildConfig.VERSION_NAME
            this.paintInfo = paintInfo
            colors = AnycubicGCodeProcessor.formatMappedPalette(remappedTools, physicalSlots, true)
            materials = AnycubicGCodeProcessor.formatMappedPalette(remappedTools, physicalSlots, false)
            val thumbnails = createAnycubicThumbnails(previewPng)
            smallThumbnail = thumbnails.first
            largeThumbnail = thumbnails.second
        }
        return try {
            if (AnycubicGCodeProcessor.rewrite(source, prepared, scan, options)) prepared
            else {
                showPreparationFailure(fragment)
                null
            }
        } catch (error: IllegalArgumentException) {
            showAnycubicMappingError(fragment, R.plurals.MmuAssignmentToolMismatch, remappedTools.size + 1)
            prepared.delete()
            null
        } catch (error: IOException) {
            Log.e("SliceMenu", "Could not rewrite G-code for Anycubic upload", error)
            prepared.delete()
            showPreparationFailure(fragment)
            null
        }
    }

    private fun prepareQidiUpload(
        source: File,
        printer: ConfigObject,
        preview: ByteArray?,
        uploadName: String,
    ): File {
        val parent = source.parentFile ?: throw IOException("The temporary G-code folder is unavailable.")
        val safeName = uploadName.replace(Regex("[/\\\\]"), "_")
        val destination = File(parent, "qidi_$safeName")
        QidiGCode3mfPackager.create(
            sourceGcode = source,
            destination = destination,
            previewPng = preview,
            printer = printer,
            filamentSlots = Prefs.getFilamentSlots(),
            clientVersion = BuildConfig.VERSION_NAME,
            displayName = safeName,
        )
        return destination
    }

    private fun isAnycubicKobra3(printer: ConfigObject): Boolean =
        printer.get("printer_model")?.lowercase(Locale.ROOT)?.startsWith("anycubic kobra 3") == true

    /** Captures the actual rendered model from the build-plate viewport for printer thumbnails. */
    private fun captureWorkspacePreview(fragment: BedFragment, callback: (ByteArray?) -> Unit) {
        val view = fragment.glView
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || view == null || view.width < 1 || view.height < 1) {
            callback(null)
            return
        }
        val scale = minOf(1f, 512f / maxOf(view.width, view.height).toFloat())
        val width = maxOf(1, (view.width * scale).toInt())
        val height = maxOf(1, (view.height * scale).toInt())
        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (error: OutOfMemoryError) {
            Log.w("SliceMenu", "Could not allocate the printer preview image", error)
            callback(null)
            return
        }
        val handler = Handler(Looper.getMainLooper())
        val source = view.renderer.gcodeResult
        var originalRange: androidx.core.util.Pair<Long, Long>? = null
        var finished = false
        var copyRequested = false
        lateinit var timeout: Runnable
        fun finish(preview: ByteArray?) {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeout)
            view.queueEvent {
                if (view.renderer.gcodeResult === source) {
                    val viewer = view.renderer.viewer
                    val selected = source?.previewSelection
                    if (viewer != null && originalRange != null) {
                        viewer.setLayersViewRange(selected?.from?.toLong()?.minus(1) ?: originalRange!!.first,
                            selected?.to?.toLong()?.minus(1) ?: originalRange!!.second)
                        viewer.applyPerformanceMode(Prefs.isPerformanceModeEnabled() || Prefs.isLowRamDevice())
                        view.requestRender()
                    }
                }
            }
            if (!copyRequested) bitmap.recycle()
            callback(preview)
        }
        timeout = Runnable { finish(null) }
        handler.postDelayed(timeout, 4_000)
        val capture = Runnable {
            if (finished) return@Runnable
            copyRequested = true
            try {
            PixelCopy.request(view, bitmap, { result ->
                if (finished) { bitmap.recycle(); return@request }
                if (result != PixelCopy.SUCCESS) {
                    bitmap.recycle()
                    Log.w("SliceMenu", "Printer preview capture failed with PixelCopy status $result")
                    finish(null)
                    return@request
                }
                try {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    finish(stream.toByteArray())
                } catch (error: Exception) {
                    Log.w("SliceMenu", "Could not encode the printer preview image", error)
                    finish(null)
                } finally {
                    bitmap.recycle()
                }
            }, handler)
        } catch (error: RuntimeException) {
            copyRequested = false
            Log.w("SliceMenu", "Could not capture the printer preview image", error)
            finish(null)
        }
        }
        view.queueEvent {
            val viewer = view.renderer.viewer
            if (source != null && view.renderer.gcodeResult === source && viewer != null && viewer.getLayersCount() > 0) {
                originalRange = viewer.layersViewRange
                // Printer thumbnails always show the complete object, independently
                // of the selected preview part. Restore the user's part after copying.
                viewer.applyPerformanceMode(true)
                viewer.setLayersViewRange(0, viewer.getLayersCount() - 1)
                view.renderer.afterNextFrame(capture)
                view.requestRender()
            } else handler.post(capture)
        }
    }

    private fun showGCodeIntegrityFailure(fragment: BedFragment, result: GCodeIntegrityValidator.Result) {
        ViewUtils.postOnMainThread {
            val context = fragment.getContext() ?: return@postOnMainThread
            val message = when (result.failure) {
                GCodeIntegrityValidator.Failure.LAYER_COUNT_MISMATCH -> {
                    val actualLayerStarts = context.resources.getQuantityString(
                        R.plurals.GCodeIntegrityLayerStarts,
                        result.actualLayers,
                        result.actualLayers,
                    )
                    context.resources.getQuantityString(
                        R.plurals.GCodeIntegrityLayerMismatch,
                        result.expectedLayers,
                        result.expectedLayers,
                        actualLayerStarts,
                    )
                }
                GCodeIntegrityValidator.Failure.NUMBERING_GAP -> context.getString(R.string.GCodeIntegrityNumberingGap)
                GCodeIntegrityValidator.Failure.LAST_LAYER_HAS_NO_EXTRUSION ->
                    context.getString(R.string.GCodeIntegrityNoLastLayerExtrusion)
                GCodeIntegrityValidator.Failure.END_BEFORE_LAST_LAYER ->
                    context.getString(R.string.GCodeIntegrityEndBeforeLastLayer)
                GCodeIntegrityValidator.Failure.MISSING_COMPLETION_MARKER ->
                    context.getString(R.string.GCodeIntegrityMissingCompletion)
                GCodeIntegrityValidator.Failure.HEIGHT_MISMATCH ->
                    context.getString(R.string.GCodeIntegrityHeightMismatch)
                else -> context.getString(R.string.GCodeIntegrityGeneric)
            }
            BeamAlertDialogBuilder(context)
                .setTitle(R.string.GCodeIntegrityTitle)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun showAnycubicMappingError(fragment: BedFragment, message: Int, count: Int? = null) {
        ViewUtils.postOnMainThread {
            val context = fragment.getContext() ?: return@postOnMainThread
            val text = if (count == null) context.getString(message)
            else context.resources.getQuantityString(message, count, count)
            BeamAlertDialogBuilder(context)
                .setTitle(R.string.MenuSliceSendToPrinterFailed)
                .setMessage(text)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    /** Encodes real viewport pixels in Anycubic's two expected preview sizes. No branded placeholder is sent. */
    private fun createAnycubicThumbnails(previewPng: ByteArray?): Pair<String, String> {
        if (previewPng == null || previewPng.isEmpty()) return "" to ""
        val source = try {
            BitmapFactory.decodeByteArray(previewPng, 0, previewPng.size)
        } catch (error: OutOfMemoryError) {
            Log.w("SliceMenu", "Could not decode the printer preview image", error)
            null
        } ?: return "" to ""
        return try {
            createAnycubicThumbnail(source, 230, 110, false) to
                createAnycubicThumbnail(source, 512, 512, true)
        } finally {
            source.recycle()
        }
    }

    private fun createAnycubicThumbnail(source: Bitmap, width: Int, height: Int, isTop: Boolean): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val png = ByteArrayOutputStream()
        try {
            val targetRatio = width.toFloat() / height.toFloat()
            val sourceRatio = source.width.toFloat() / source.height.toFloat()
            val sourceRect = if (sourceRatio > targetRatio) {
                val cropWidth = (source.height * targetRatio).toInt().coerceIn(1, source.width)
                val left = (source.width - cropWidth) / 2
                Rect(left, 0, left + cropWidth, source.height)
            } else {
                val cropHeight = (source.width / targetRatio).toInt().coerceIn(1, source.height)
                val top = (source.height - cropHeight) / 2
                Rect(0, top, source.width, top + cropHeight)
            }
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(35, 39, 45))
            canvas.drawBitmap(
                source,
                sourceRect,
                Rect(0, 0, width, height),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG),
            )
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, png)
        } finally {
            bitmap.recycle()
        }
        val encoded = Base64.encodeToString(png.toByteArray(), Base64.NO_WRAP)
        return AnycubicGCodeProcessor.formatThumbnailBlock(width, height, encoded, isTop)
    }
}
