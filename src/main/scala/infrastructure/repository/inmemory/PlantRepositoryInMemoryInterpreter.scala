package infrastructure.repository
package inmemory

import cats.Applicative
import cats.implicits._

import domain.{Plant, PlantId, PlantRepository}

import scala.collection.concurrent.TrieMap


class PlantRepositoryInMemoryInterpreter[F[_]: Applicative] extends PlantRepository[F] {

  private val cache = new TrieMap[PlantId, Plant]


  def get(plantId: PlantId): F[Option[Plant]] = cache.get(plantId).pure[F]

  def findByName(name: String): F[Option[Plant]] = cache.values.find(p => p.name == name).pure[F]
}
