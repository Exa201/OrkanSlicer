package ru.ytkab0bp.sapil

import com.google.gson.Gson

class APILibraryConfig {
    @JvmField
    var defaultRequestType: RequestType = RequestType.GET

    @JvmField
    var gson: Gson = Gson()

    fun setDefaultRequestType(defaultRequestType: RequestType): APILibraryConfig {
        this.defaultRequestType = defaultRequestType
        return this
    }

    fun setGson(gson: Gson): APILibraryConfig {
        this.gson = gson
        return this
    }
}
