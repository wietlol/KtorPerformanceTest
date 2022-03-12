import kotlinx.cinterop.*
import platform.posix.size_t
import libcurl.*

// example taken from https://github.com/JetBrains/kotlin/tree/master/kotlin-native/samples/libcurl/src/libcurlMain/kotlin
class CUrl(url: String)
{
	private val stableRef = StableRef.create(this)
	
	private val curl = curl_easy_init()
	
	init
	{
		curl_easy_setopt(curl, CURLOPT_URL, url)
		val header = staticCFunction(::header_callback)
		curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header)
		curl_easy_setopt(curl, CURLOPT_HEADERDATA, stableRef.asCPointer())
		val writeData = staticCFunction(::write_callback)
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeData)
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, stableRef.asCPointer())
	}
	
	val header = Event<String>()
	val body = Event<String>()
	
	fun nobody()
	{
		curl_easy_setopt(curl, CURLOPT_NOBODY, 1L)
	}
	
	fun fetch()
	{
		val res = curl_easy_perform(curl)
		if (res != CURLE_OK)
			println("curl_easy_perform() failed: ${curl_easy_strerror(res)?.toKString()}")
	}
	
	fun close()
	{
		curl_easy_cleanup(curl)
		stableRef.dispose()
	}
}

fun CPointer<ByteVar>.toKString(length: Int): String
{
	val bytes = this.readBytes(length)
	return bytes.decodeToString()
}

fun header_callback(buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer?): size_t
{
	if (buffer == null) return 0u
	if (userdata != null)
	{
		val header = buffer.toKString((size * nitems).toInt()).trim()
		val curl = userdata.asStableRef<CUrl>().get()
		curl.header(header)
	}
	return size * nitems
}


fun write_callback(buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer?): size_t
{
	if (buffer == null) return 0u
	if (userdata != null)
	{
		val data = buffer.toKString((size * nitems).toInt()).trim()
		val curl = userdata.asStableRef<CUrl>().get()
		curl.body(data)
	}
	return size * nitems
}

typealias EventHandler<T> = (T) -> Unit
class Event<T : Any> {
	private var handlers = emptyList<EventHandler<T>>()
	
	fun subscribe(handler: EventHandler<T>) {
		handlers += handler
	}
	
	fun unsubscribe(handler: EventHandler<T>) {
		handlers -= handler
	}
	
	operator fun plusAssign(handler: EventHandler<T>) = subscribe(handler)
	operator fun minusAssign(handler: EventHandler<T>) = unsubscribe(handler)
	
	operator fun invoke(value: T) {
		var exception: Throwable? = null
		for (handler in handlers) {
			try {
				handler(value)
			} catch (e: Throwable) {
				exception = e
			}
		}
		exception?.let { throw it }
	}
}
