package expenserecorder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import pdi.jwt._

import scala.io.StdIn
import scala.util.{Failure, Success}

object ExpenseRecorder {

  def main(args: Array[String]) {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val expenseCli = new ExpenseCli("mongodb://localhost:27017")
    val jwtKey = sys.env("EXPENSE_RECORDER_JWT_SECRET")
    val authCli = new AuthCli("mongodb://localhost:27017")

    def verifyCookie(loginToken: String) = {
      Jwt.decodeRaw(loginToken, jwtKey, Seq(JwtAlgorithm.HS256))
    }


    val route =
      path("expense") {
          post {
            cookie ("loginToken") { loginToken => {
              val verifyResult = verifyCookie(loginToken.value)
              verifyResult match {
                case Success(emailID) => formFields('itemName, 'amount, 'comment, 'category) {
                  (itemName, amount, comment, category) => {
                    expenseCli.addExpense(emailID, Integer.parseInt(amount), itemName, comment,
                      ExpenseCategory.withName(category))
                    complete(
                      HttpEntity(
                        ContentTypes.`text/plain(UTF-8)`,
                        // transform each number to a chunk of bytes
                        "Recorded " + amount + " spent on " + itemName
                      )
                    )
                  }
                }
                case Failure(exception) => complete(StatusCodes.Unauthorized)
              }

            }
          }
        }
      } ~ path ("user") {
        post {
          formFields('emailID, 'firstName, 'lastName, 'password) {
            (emailID, firstName, lastName, password) => {
              authCli.register(emailID, firstName, lastName, password)
              complete(
                HttpEntity(
                  ContentTypes.`text/plain(UTF-8)`,
                  // transform each number to a chunk of bytes
                  "Registered " + firstName + " " + lastName
                )
              )
            }

          }
        }
      } ~ path("login") {
      post {
        formFields('emailID, 'password) {
          (emailID, password) => {
            val result = authCli.verify(emailID, password)
            result match {
              case e : Some[String] =>
                val token = Jwt.encode("""{"user":""" + emailID + "}", jwtKey, JwtAlgorithm.HS256)
                setCookie(HttpCookie("loginToken", token)) {
                  complete(StatusCodes.OK)
                }
              case _ => complete(StatusCodes.Unauthorized)
            }
          }
        }
      }
    }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
