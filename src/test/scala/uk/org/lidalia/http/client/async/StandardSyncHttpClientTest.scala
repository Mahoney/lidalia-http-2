package uk.org.lidalia.http.client

import org.joda.time.{DateTimeZone, DateTime}
import org.scalatest.FunSuite
import uk.org.lidalia.http.client.StandardSyncHttpClient.get
import uk.org.lidalia.http.core.MediaType.`text/plain`
import uk.org.lidalia.http.core.Code
import uk.org.lidalia.http.core.headerfields.{ContentType, Date}
import uk.org.lidalia.net2.Url
import uk.org.lidalia.slf4jext.Level
import uk.org.lidalia.slf4jtest.TestLoggerFactory
import uk.org.lidalia.stubhttp.{DSL, StubHttpServerFactory}
import uk.org.lidalia.support.WithResourceTest

class StandardSyncHttpClientTest extends FunSuite with WithResourceTest {

  TestLoggerFactory.getInstance().setPrintLevel(Level.INFO)

  test("can get bytes", StubHttpServerFactory()) { server =>
    server.stub(
      DSL.get("/foo")
      .returns(
        200,
        Date:= "Sun, 06 Nov 1994 08:49:37 GMT",
        ContentType:= "text/plain"
      )(
        "Some text"
      )
    )

    def response = get(Url(server.localAddress.toString ++ "/foo"))

    assert(response.code == Code(200))
    assert(response.contentType.contains(`text/plain`))
    assert(response.date.contains(new DateTime("1994-11-06T08:49:37.000Z").withZone(DateTimeZone.forID("GMT"))))
    assert(response.entityString == "Some text")
  }
}
