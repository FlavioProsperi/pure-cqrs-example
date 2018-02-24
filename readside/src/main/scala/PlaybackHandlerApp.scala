import cats.effect.{Effect, IO}
import config.{ApplicationConfig, DatabaseConfig}
import fs2.StreamApp.ExitCode
import fs2.{Pipe, Scheduler, Stream, StreamApp}
import interpreter.doobie.EventLogDoobieInterpreter

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PlaybackHandlerApp extends StreamApp[IO] {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    // TODO: Search what happens with drain? why without it we don't consume the stream I miss something here...
    createStream[IO](args, requestShutdown).drain.as(ExitCode.Success)


  def loopWithDelay[F[_]: Effect, A](program: Stream[F,A], every: FiniteDuration)(implicit ec: ExecutionContext) : Stream[F, A] = {
       Scheduler[F](2).flatMap(f => (program ++ f.sleep_[F](every)).repeat )
  }

  def log[F[_]: Effect,A](prefix: String): Pipe[F, A, A] = _.evalMap(a => Effect[F].delay { println(s"[$prefix - ${Thread.currentThread.getName}] $a "); a})

  def createStream[F[_] : Effect](args: List[String], shutdown: IO[Unit]): Stream[F,Unit] = {
    for {
      conf <- Stream.eval(ApplicationConfig.load[F]("read-side-server"))
      xa <- Stream.eval(DatabaseConfig.dbTransactor[F](conf.db))
      eventLog = EventLogDoobieInterpreter[F](xa)
      readLogProgram =  eventLog.consume().through(log("consumer"))
      _ <- loopWithDelay(readLogProgram, 10.second)
    } yield ()
  }
}


