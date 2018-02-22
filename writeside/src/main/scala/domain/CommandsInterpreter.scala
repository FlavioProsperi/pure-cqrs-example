package domain

import cats.Functor
import cats.data.EitherT
import cats.effect.Effect
import domain.events._
import domain.validations._

class CommandsInterpreter[F[_] : Effect](elog: EventLogAlgebra[F], v: ValidationAlgebra[F]) extends CommandsAlgebra[F] {
  // TODO decide if we will make these effect (F[_]) extension...
  // Helper experiment for EitherT help operators
  implicit class EitherTLiftOp[G[_]: Functor, B](f: G[B]) {
    def liftF[A]: EitherT[G, A, B] = EitherT.liftF[G,A,B](f)
  }

  def create(name: String, country: String): F[Either[ValidationError, Event]] = {
    val res: EitherT[F, ValidationError, Event] = for {
      _   <- EitherT(v.checkPlantDoesNotExist(name))
      uid <- elog.generateUID().liftF
      _   <- v.put((uid, name)).liftF
      event = PlantCreated(uid, name, country)//.asInstanceOf[Event].asJson
      ev  <- elog.append(event).liftF
    } yield ev
    res.value
  }

  def delete(id: PlantId): F[Either[ValidationError, Event]] = {
    val res: EitherT[F, ValidationError, Event] = for {
      _   <- EitherT(v.checkPlantExists(id))
      _   <- v.delete(id).liftF
      ev  <- elog.append(PlantDeleted(id.value)).liftF
    } yield ev
    res.value
  }
}

object CommandsInterpreter {
  def apply[F[_] : Effect](eventLog: EventLogAlgebra[F], validation: ValidationAlgebra[F]) =
    new CommandsInterpreter[F](eventLog, validation)
}