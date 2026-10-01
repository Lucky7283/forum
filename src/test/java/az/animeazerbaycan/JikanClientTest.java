package az.animeazerbaycan;
import az.animeazerbaycan.animeimporter.JikanClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
class JikanClientTest {
 @Test void fetchesFullRecordAndDeserializesJikanFields() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); var calls=new AtomicInteger();
  server.createContext("/v4/anime/1/full",exchange->{
   calls.incrementAndGet(); byte[] body="{\"data\":{\"mal_id\":1,\"title\":\"Cowboy Bebop\",\"images\":{\"jpg\":{\"large_image_url\":\"https://example.com/image.jpg\"}},\"extra\":true}}".getBytes(StandardCharsets.UTF_8);
   exchange.getResponseHeaders().set("Content-Type","application/json"); exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); exchange.close();
  });
  server.start();
  try { var data=new JikanClient("http://127.0.0.1:"+server.getAddress().getPort()+"/v4").fetch(1); assertThat(data.malId()).isEqualTo(1); assertThat(data.images().jpg().largeImageUrl()).isEqualTo("https://example.com/image.jpg"); assertThat(calls).hasValue(1); } finally { server.stop(0); }
 }
 @Test void deserializesPageAndSendsPaginationParameters() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  var query=new java.util.concurrent.atomic.AtomicReference<String>();
  server.createContext("/v4/anime",exchange->{
   query.set(exchange.getRequestURI().getQuery());
   byte[] body="{\"pagination\":{\"current_page\":2,\"has_next_page\":false},\"data\":[{\"mal_id\":26,\"title\":\"Original\"}]}".getBytes(StandardCharsets.UTF_8);
   exchange.getResponseHeaders().set("Content-Type","application/json"); exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); exchange.close();
  });
  server.start();
  try {
   var page=new JikanClient("http://127.0.0.1:"+server.getAddress().getPort()+"/v4").fetchPage(2);
   assertThat(page.data().getFirst().malId()).isEqualTo(26);
   assertThat(page.pagination().hasNextPage()).isFalse();
   assertThat(query.get()).isEqualTo("page=2");
  } finally {server.stop(0);}
 }
}
