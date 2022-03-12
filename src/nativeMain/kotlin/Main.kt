import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

val url = "http://127.0.0.1:8080/test"

fun main()
{
	val runs = 100
	var curlClient: CUrl? = null
	val createCurlClientDuration = measureTimeMillis {
		curlClient = createCurlClient()
	}
	
	val ktorClient = HttpClient()
	
	println("curl-reuse-client:")
	println("init:    $createCurlClientDuration")
	report(testCurl(runs, curlClient!!))
	println()
	println("curl-new-clients:")
	report(testCurl2(runs))
	println()
	println("ktor:")
	report(testKtor(runs, ktorClient))
}

fun report(results: List<Long>)
{
	println("total:   ${results.sum()}")
	println("average: ${results.average()}")
	println("min:     ${results.minOrNull()}")
	println("max:     ${results.maxOrNull()}")
}

fun createCurlClient(): CUrl =
	CUrl(url).apply {
		body.subscribe {
//			println(it)
		}
	}

fun testCurl(runs: Int, client: CUrl): List<Long> =
	(0 until runs)
		.map {
			measureTimeMillis {
				runCurl(client)
			}
		}
		.toList()

fun testCurl2(runs: Int): List<Long> =
	(0 until runs)
		.map {
			measureTimeMillis {
				runCurl(createCurlClient())
			}
		}
		.toList()

private fun runCurl(client: CUrl)
{
	client.fetch()
}

fun testKtor(runs: Int, client: HttpClient): List<Long> =
	runBlocking {
		(0 until runs)
			.map {
				measureTimeMillis {
					runKtor(client)
				}
			}
			.toList()
	}

private suspend fun runKtor(client: HttpClient)
{
	val response = client.get<HttpResponse>(url)
	val body = response.receive<String>()
//	println(body)
}

