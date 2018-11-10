package expenserecorder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge, HttpCredentials}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.io.StdIn

object ExpenseRecorder {

  def main(args: Array[String]) {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val expenseCli = new ExpenseCli("mongodb://localhost:27017")
    val authCli = new AuthCli("mongodb://localhost:27017")

//    def myUserPassAuthenticator(credentials:HttpCredentials): Future[Option[String]] =
//      credentials match {
////        case p @ Credentials.Provided(id) => authCli.verify(p.identifier, "")
////          Future {
////            // potentially
////            if (p.verify("p4ssw0rd")) Some(id)
////            else None
////          }
//        case Some(BasicHttpCredentials(username, password)) => Future.successful(None)
//        case _ => Future.successful(None)
//      }

    def myUserPassAuthenticator(credentials: Option[HttpCredentials]): Future[AuthenticationResult[String]] =
      Future {
        credentials match {
          case Some(creds)  => {
            val basicCreds = creds.asInstanceOf[BasicHttpCredentials]
            val res = authCli.verify(basicCreds.username, basicCreds.password)
            res match {
              case Some(username) => Right(username)
              case _ => Left(HttpChallenge("MyAuth", Some("MyRealm")))
            }
          }
          case _ => Left(HttpChallenge("MyAuth", Some("MyRealm")))
        }
      }

    val route =
      path("expense") {
        authenticateOrRejectWithChallenge (myUserPassAuthenticator _) { userName =>
          post {
            formFields('itemName, 'amount, 'comment, 'category) {
              (itemName, amount, comment, category) => {
                //cli.addExpense(userName, Integer.parseInt(amount), itemName, comment, ExpenseCategory.withName(category))
                complete(
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    // transform each number to a chunk of bytes
                    "Recorded " + amount + " spent on " + comment
                  )
                )
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
      }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
