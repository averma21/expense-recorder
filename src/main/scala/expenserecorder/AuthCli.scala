package expenserecorder

import java.util.NoSuchElementException

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.async.client.{Observer, Subscription}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.{DEFAULT_CODEC_REGISTRY, Macros}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{Completed, MongoClient, MongoCollection}
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.Future

case class User(emailID: String, passHash: String, firstName: String, lastName: String) {
  override def toString: String = {
    "emailID: " + emailID + ", passHash: " + passHash+ ", firstName: " + firstName + ", lastName: " + lastName
  }
}

object AuthCli {
  private val DB_NAME = "expenserecorder"
  private val COLLECTION_USERS = "users"
}

class AuthCli(val mongoURL: String) {

  import AuthCli._

  implicit val system : ActorSystem = ActorSystem()
  implicit val mat : ActorMaterializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  private val codecRegistry = fromRegistries(fromProviders(Macros.createCodecProvider[User]),
    DEFAULT_CODEC_REGISTRY)

  // #init-connection
  private val client = MongoClient(mongoURL)
  private val db = client.getDatabase(DB_NAME)

  private val credsColl : MongoCollection[User] = db.getCollection[User](COLLECTION_USERS)
    .withCodecRegistry(codecRegistry)

  def verify(emailID: String, passWord: String): Option[String] = {
    val f = credsColl.find(equal("emailID", emailID)).head().
      withFilter(creds => BCrypt.checkpw(passWord, creds.passHash))
    while(!f.isCompleted) {}
    try {
      Some(f.value.get.get.emailID)
    } catch {
      case e: NoSuchElementException => None
    }
  }

  def register(emailID: String, firstName: String, lastName: String, password: String):Any = {
    val user: User = User(emailID, BCrypt.hashpw(password, BCrypt.gensalt()), firstName, lastName)
    val f : Future[User] = credsColl.find(equal("emailID", emailID)).head()
    while (!f.isCompleted) {}
    if (f.value.get.get == null) {
      credsColl.insertOne(user).subscribe(new Observer[Completed] {
        override def onSubscribe(subscription: Subscription): Unit = {
          println("Subscribed...")
        }

        override def onNext(result: Completed): Unit = {
          println("On next " + result)
        }

        override def onError(e: Throwable): Unit = {
          println(e)
        }

        override def onComplete(): Unit = {
          println("Completed")
        }

      })
    }
    else
      throw new Exception("User " + emailID + " already exists")
  }

}
