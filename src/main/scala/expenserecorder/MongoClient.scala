package expenserecorder

import java.util.{Calendar, Date}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.async.client.{Observer, Subscription}

import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{Completed, MongoClient, MongoCollection}

class MongoClient {

  object Category extends Enumeration {

    type Category = Value

    val cab : Value = Value("CAB")
    val grocery : Value = Value("GROCERY")
    val train : Value = Value("TRAIN")
    val flight : Value = Value("FLIGHT")
    val clothing : Value = Value("CLOTHING")
    val electronics : Value = Value("ELECTRONICS")
    val fuel : Value = Value("FUEL")
    val health : Value = Value("HEALTH")
    val sports: Value = Value("SPORTS")
    val outsideFood : Value = Value("OUTSIDE_FOOD")
    val donation : Value = Value("DONATION")
    val vehicleMaintenance : Value = Value("VEHICLE_MAINTENANCE")
    val homeMaintenance : Value = Value("HOME_MAINTENANCE")
    val booksAndCourses : Value = Value("BOOKS_AND_COURSES")
    val miscellaneous : Value = Value("MISCELLANEOUS")
  }

  case class Expense (date : Date, amount : Double, itemName : String, comment : String,
                      category: String)

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  import system.dispatcher

  val codecRegistry = fromRegistries(fromProviders(Macros.createCodecProvider[Expense]), DEFAULT_CODEC_REGISTRY)

  // #init-connection
  private val client = MongoClient(s"mongodb://localhost:27017")
  private val db = client.getDatabase("expenserecorder")
  private val expensesColl : MongoCollection[Expense] = db.getCollection[Expense]("expenses-amrit").withCodecRegistry(codecRegistry)

  println("INserting...")

  val insertObservable = expensesColl.insertOne(Expense(Calendar.getInstance().getTime, 190.30, "Something", "Some comm", Category.cab.toString))

  insertObservable.subscribe(new Observer[Completed] {
    override def onSubscribe(subscription: Subscription): Unit = {
      println("Subscribed...")
      println(subscription)
    }

    override def onNext(result: Completed): Unit = {
      println("On next" + result)
    }

    override def onError(e: Throwable): Unit = {
      println(e)
    }

    override def onComplete(): Unit = {
      println("Completed")
    }
  })

}