package stoickit.db.quotes

import stoickit.db.generic._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}
import scala.util.Random

import stoickit.interface.quotes.{Quote, QuotesProvider}

object Implicits {
  implicit val quotesDb = QuotesDb
}

class Quotes(tag: Tag) extends Table[Quote](tag, "quotes") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def author = column[String]("author")
  def content = column[String]("content")
  def * = (id, author, content) <> (Quote.tupled, Quote.unapply)
}

object QuotesDb extends QuotesProvider {
  val quotes = TableQuery[Quotes]
  import SqlDb._
  val random = Random

  def init = db.run(quotes.schema.create)

  def addQuote(quote: Quote) = db.run(quotes += quote)
  def create(quote: Quote) = db.run(quotes += quote)
  def randomQuote: Future[Option[Quote]] = {
    db.run(quotes.result).map({quoteSeq: Seq[Quote] => quoteSeq.nonEmpty match {
      case false => None
      case true => Some(quoteSeq(random.nextInt(quoteSeq.length)))
    }
    })
  }
  def stream(author: Option[String] = None) = db.stream(author match {
    case None => quotes.result
    case Some(authorStr) => quotes.filter(_.author === author).result
  })

  def get(id: Int): Future[Option[Quote]] = getQuote(id)
  def getQuote(id: Int): Future[Option[Quote]] = db.run(quotes.filter(_.id === id).result.headOption)

  def getQuotesBy(author: String): Future[Seq[Quote]] = db.run(quotes.filter(_.author === author).result)
}