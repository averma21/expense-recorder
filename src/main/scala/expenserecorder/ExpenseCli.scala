package expenserecorder

import java.util.{Calendar, Date}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.async.client.{Observer, Subscription}
import expenserecorder.ExpenseCategory.ExpenseCategory
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{Completed, MongoClient, MongoCollection}


case class Expense (userid: String, date : Date, amount : Double, itemName : String, comment : String,
                    category: String) {
  override def toString: String = {
    "UserID: " + userid + ", date: " + date + ", amount: " + amount + ", itemName: " + itemName +
    ", comment: " + comment + ", category: " + category
  }
}

object ExpenseCli {
  private val DB_NAME = "expenserecorder"
  private val COLLECTION_EXPENSES = "expenses"
}

class ExpenseCli(val mongoURL: String) {

  import ExpenseCli._

  implicit val system : ActorSystem = ActorSystem()
  implicit val mat : ActorMaterializer = ActorMaterializer()
  private val codecRegistry = fromRegistries(fromProviders(Macros.createCodecProvider[Expense]),
    DEFAULT_CODEC_REGISTRY)

  // #init-connection
  private val client = MongoClient(mongoURL)
  private val db = client.getDatabase(DB_NAME)
  private val expensesColl : MongoCollection[Expense] = db.getCollection[Expense](COLLECTION_EXPENSES)
    .withCodecRegistry(codecRegistry)

  def addExpense(userid: String, amount : Double, itemName : String, comment : String, category: ExpenseCategory): Unit = {

    val expense = Expense(userid, Calendar.getInstance().getTime, amount, itemName, comment, category.toString)

    println("Inserting Expense {" + expense + "}")

    val insertObservable = expensesColl.insertOne(expense)

    insertObservable.subscribe(new Observer[Completed] {
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

}