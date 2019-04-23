import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import engine.Engine
import server.Server
import scala.concurrent.ExecutionContextExecutor

object Main{
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val engine: Engine = new Engine()
    val server: Server = new Server(engine)
  }
}
