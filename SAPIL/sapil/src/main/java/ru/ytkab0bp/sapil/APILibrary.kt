package ru.ytkab0bp.sapil

import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.HashMap
import java.util.concurrent.Executors

object APILibrary {
    private val supportedProtocols = Arrays.asList("http", "https")
    private val ioPool = Executors.newCachedThreadPool()

    @Volatile
    private var config = APILibraryConfig()

    /** Sets the global API library configuration. */
    @JvmStatic
    fun setConfig(config: APILibraryConfig) {
        this.config = config
    }

    /** Creates a dynamic API runner from an annotated interface. */
    @Suppress("UNCHECKED_CAST", "rawtypes")
    @JvmStatic
    fun <T : APIRunner> newRunner(clz: Class<T>, config: APIRunner.RunnerConfig?): T {
        val proxy = java.lang.reflect.Proxy.newProxyInstance(
            clz.classLoader,
            arrayOf<Class<*>>(clz)
        ) { _, invokedMethod, callArgs ->
            val request = invokedMethod.getAnnotation(Method::class.java)
                ?: throw UnsupportedOperationException("Method ${invokedMethod.name} is unsupported.")

            var url = request.value
            if (config?.getNamingTransformPolicy() != null) {
                val policy = config.getNamingTransformPolicy()!!
                url = url.replace(Regex(policy.first), policy.second)
            }
            val activeConfig = this.config
            val type = if (request.requestType != RequestType._DEFAULT) {
                request.requestType
            } else {
                activeConfig.defaultRequestType
            }

            var callback: APICallback<Any?>? = null
            var callbackType: Type? = null
            var enclosure: APIEnclosure? = null
            val params = HashMap<String, String>()
            val headers = HashMap<String, String>()
            headers["User-Agent"] = config?.getDefaultUserAgent() ?: APIRunner.DEFAULT_USER_AGENT
            config?.getDefaultHeaders()?.let(headers::putAll)

            val args = callArgs ?: emptyArray()
            for (i in args.indices) {
                val argClass = invokedMethod.parameterTypes[i]
                val arg = args[i]
                if (argClass.isAssignableFrom(APICallback::class.java)) {
                    if (callback == null) {
                        callbackType = (invokedMethod.genericParameterTypes[i] as ParameterizedType)
                            .actualTypeArguments[0]
                        @Suppress("UNCHECKED_CAST")
                        callback = arg as APICallback<Any?>?
                    } else {
                        throw UnsupportedOperationException("Method confusion: more than one callback")
                    }
                } else if (argClass.isAssignableFrom(APIEnclosure::class.java)) {
                    if (enclosure == null) {
                        enclosure = arg as APIEnclosure?
                    } else {
                        throw UnsupportedOperationException("Method confusion: more than one enclosure")
                    }
                } else {
                    var key: String? = null
                    var isHeader = false
                    val annotations = invokedMethod.parameterAnnotations[i]
                    for (annotation in annotations) {
                        when (annotation) {
                            is Arg -> {
                                key = annotation.value
                                break
                            }
                            is Header -> {
                                key = annotation.value
                                isHeader = true
                                break
                            }
                        }
                    }

                    val value = when (arg) {
                        is Boolean -> if (config == null || config.getBooleansAsInt()) {
                            if (arg) "1" else "0"
                        } else {
                            arg.toString()
                        }
                        is List<*> -> activeConfig.gson.toJson(arg)
                        else -> arg.toString()
                    }
                    if (isHeader) {
                        headers[key!!] = value
                    } else if (key == null) {
                        val marker = url.indexOf("{}")
                        if (marker == -1) {
                            throw UnsupportedOperationException("Method confusion: unknown argument without name")
                        }
                        url = url.substring(0, marker) + URLEncoder.encode(value, "UTF-8") + url.substring(marker + 2)
                    } else {
                        params[key] = value
                    }
                }
            }

            var fullUrl = if (config != null) {
                config.getBaseURL() + config.getPathPrefix() + url
            } else {
                url
            }
            if (type == RequestType.GET) {
                val query = StringBuilder(fullUrl).append('?')
                var first = true
                for ((key, value) in params) {
                    if (!first) query.append('&') else first = false
                    query.append(URLEncoder.encode(key, "UTF-8"))
                        .append('=')
                        .append(URLEncoder.encode(value, "UTF-8"))
                }
                fullUrl = query.toString()
            }

            val urlObject = URL(fullUrl)
            if (!supportedProtocols.contains(urlObject.protocol)) {
                throw UnsupportedOperationException("Unsupported protocol: ${urlObject.protocol}")
            }
            if (urlObject.host.isNullOrEmpty()) {
                throw UnsupportedOperationException("Host is null or empty")
            }

            val finalCallback = callback
            val finalCallbackType = callbackType
            val handle = APIRequestHandleImpl(ioPool.submit {
                try {
                    val connection = urlObject.openConnection() as HttpURLConnection
                    connection.requestMethod = type.name
                    connection.doOutput = type != RequestType.GET
                    for ((key, value) in headers) connection.addRequestProperty(key, value)

                    if (connection.doOutput) {
                        val output: OutputStream = connection.outputStream
                        if (params.size == 1 && params.containsKey("")) {
                            output.write(params[""]!!.toByteArray(StandardCharsets.UTF_8))
                        } else {
                            val body = StringBuilder()
                            var first = true
                            for ((key, value) in params) {
                                if (!first) body.append('&') else first = false
                                body.append(URLEncoder.encode(key, "UTF-8"))
                                    .append('=')
                                    .append(URLEncoder.encode(value, "UTF-8"))
                            }
                            output.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                        }
                        // TODO: Multipart support
                        output.close()
                    }

                    var result: Any? = null
                    if (finalCallback != null) {
                        val input = connection.inputStream
                        if (finalCallbackType == InputStream::class.java) {
                            result = input
                        } else {
                            val reader = BufferedReader(InputStreamReader(input))
                            val line = reader.readLine()
                            result = when (finalCallbackType) {
                                String::class.java -> line
                                java.lang.Boolean::class.java -> java.lang.Boolean.parseBoolean(line)
                                java.lang.Integer::class.java -> Integer.parseInt(line)
                                java.lang.Float::class.java -> java.lang.Float.parseFloat(line)
                                java.lang.Long::class.java -> java.lang.Long.parseLong(line)
                                java.lang.Double::class.java -> java.lang.Double.parseDouble(line)
                                java.lang.Byte::class.java -> java.lang.Byte.parseByte(line)
                                else -> {
                                    val json = JsonParser.parseReader(reader)
                                    activeConfig.gson.fromJson<Any>(json, finalCallbackType)
                                }
                            }
                            reader.close()
                            input.close()
                        }
                    }
                    if (result != null) finalCallback?.onResponse(result)
                    pingQueue()
                } catch (e: Exception) {
                    finalCallback?.onException(e)
                }
            })

            enclosure?.addLifecycleDestroyListener(handle::cancel)
            if (invokedMethod.returnType == APIRequestHandle::class.java) handle else null
        }
        return proxy as T
    }

    private fun pingQueue() {
        // No-op for now.
    }
}
