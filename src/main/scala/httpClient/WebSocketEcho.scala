package httpClient

import java.io.IOException
import java.util.concurrent.{Executors, TimeUnit}
import java.net.{Proxy, ProxySelector}

import okhttp3._
import okhttp3.ws.{WebSocket, WebSocketCall, WebSocketListener}
import okhttp3.ws.WebSocket.{BINARY, TEXT}
import okio.{Buffer, ByteString}

/**
  * Created by iyanu on 12/13/16.
  */

class WebSocketEcho extends WebSocketListener {
  private final val writeExecutor = Executors.newSingleThreadExecutor()
  private val kind = "SparkJob"
  private val apiVersion = "apache.io/v1"
  private val protocol = "http://"

  //"wss://192.168.64.17:8443/api/v1/pods?watch=true"
  private val apiEndpoint = s"ws://127.0.0.1:8001/apis/$apiVersion/namespaces/iobidele/sparkjobs"

  def run() = {
    val client = new OkHttpClient.Builder()
      //.proxy(Proxy.NO_PROXY)
      .readTimeout(0, TimeUnit.MILLISECONDS)
      .build()


    val request = new Request.Builder()
      .url(s"$apiEndpoint?watch=true")
      .build()

    println(request)

    WebSocketCall.create(client, request).enqueue(this)

    client.dispatcher().executorService().shutdown()
  }

  override def onMessage(message: ResponseBody): Unit = {
    if (message.contentType() == TEXT) {
      println(s"Msg: ${message.string()}")
    } else {
      println(s"Msg: ${message.source().readByteString().hex()}")
    }
    message.close()
  }

  override def onClose(code: Int, reason: String): Unit = {
    println(s"CLOSE: $code $reason")
    writeExecutor.shutdown()
  }

  override def onOpen(webSocket: WebSocket, response: Response): Unit = {
    /*writeExecutor.execute(new Runnable {
      override def run() = {
        try {

        } catch {
          case e: IOException =>
            println(s"ERROR: ${e.getMessage}")
            writeExecutor.shutdown()
        }
      }
    })*/
    writeExecutor.execute(new Runnable {
      override def run(): Unit =
        println("Connection open with server")
        onMessage(response.body())
    })

  }

  override def onFailure(e: IOException, response: Response): Unit = {
    e.printStackTrace()
    writeExecutor.shutdown()
  }

  override def onPong(payload: Buffer): Unit = {
    println(s"PONG: ${payload.readUtf8()}")
  }
}

object WebSocketEchoTest extends App {
  new WebSocketEcho().run()
}
