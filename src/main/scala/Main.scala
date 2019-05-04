import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import client.Client
import engine.Engine
import server.Server

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val client: Client = new Client()
    val engine: Engine = new Engine()(client)
    val server: Server = new Server(engine)
  }
}
