package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.Headers;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JsoupPageDownloaderIT {

    private static final Duration TIMEOUT = Duration.ofMillis(500);

    // Jsoup runs on java.net.http: waiting for the headers times out with HttpTimeoutException,
    // reading the body with SocketTimeoutException.
    private static final Class<?>[] TIMEOUTS = {HttpTimeoutException.class, SocketTimeoutException.class};

    private final StubHttpServer server = StubHttpServer.start();
    private final JsoupPageDownloader downloader = new JsoupPageDownloader(TIMEOUT);

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void sends_its_user_agent_and_no_content_type() {
        server.respond("/", 200, "text/html", page("<title>Home</title>", "").getBytes(UTF_8));

        downloader.download(server.url("/"));

        Headers request = server.requests().getFirst();
        assertThat(request.getFirst("User-Agent")).isEqualTo("daily-trends-java");
        assertThat(request.containsKey("Content-Type")).isFalse();
    }

    @Test
    void follows_redirects() {
        server.redirect("/old", server.url("/new"));
        server.respond(
                "/new", 200, "text/html", page("<title>Moved here</title>", "").getBytes(UTF_8));

        Document page = downloader.download(server.url("/old"));

        assertThat(page.title()).isEqualTo("Moved here");
        assertThat(page.location()).isEqualTo(server.url("/new"));
    }

    @Test
    void parses_the_body_of_an_error_response() {
        server.respond(
                "/", 404, "text/html", page("<title>Not found</title>", "").getBytes(UTF_8));

        Document page = downloader.download(server.url("/"));

        assertThat(page.title()).isEqualTo("Not found");
    }

    @Test
    void fails_when_the_server_does_not_answer_within_the_timeout() {
        server.hang("/");

        assertThatThrownBy(() -> downloader.download(server.url("/")))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining(server.url("/"))
                .rootCause()
                .isInstanceOfAny(TIMEOUTS);
    }

    @Test
    void fails_when_the_whole_request_takes_longer_than_the_timeout() {
        List<byte[]> chunks = Collections.nCopies(10, "<p>more news</p>".getBytes(UTF_8));
        server.respondInChunks("/", "text/html", chunks, TIMEOUT.dividedBy(5));

        assertThatThrownBy(() -> downloader.download(server.url("/")))
                .isInstanceOf(UncheckedIOException.class)
                .rootCause()
                .isInstanceOfAny(TIMEOUTS);
    }

    @Test
    void fails_when_the_server_is_unreachable() {
        String url = server.url("/");
        server.close();

        assertThatThrownBy(() -> downloader.download(url)).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void accepts_a_timeout_longer_than_an_int_of_milliseconds() {
        server.respond("/", 200, "text/html", page("<title>Home</title>", "").getBytes(UTF_8));

        Document page = new JsoupPageDownloader(Duration.ofDays(30)).download(server.url("/"));

        assertThat(page.title()).isEqualTo("Home");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            charset in the header        | text/html; charset=iso-8859-15         |                                                                              | ISO-8859-15
            charset in <meta charset>    | text/html                              | <meta charset="iso-8859-15">                                                 | ISO-8859-15
            charset in <meta http-equiv> | text/html                              | <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-15">     | ISO-8859-15
            no declaration means UTF-8   | text/html                              |                                                                              | UTF-8
            the header wins over <meta>  | text/html; charset=iso-8859-15         | <meta charset="utf-8">                                                       | ISO-8859-15
            unknown header charset       | text/html; charset=x-unknown-charset   | <meta charset="iso-8859-15">                                                 | ISO-8859-15
            """)
    void decodes_the_page_with_the_declared_charset(
            String scenario, String contentType, String meta, String bodyCharset) {
        byte[] body = page(meta == null ? "" : meta, "<p>€</p>").getBytes(Charset.forName(bodyCharset));
        server.respond("/", 200, contentType, body);

        Document page = downloader.download(server.url("/"));

        assertThat(page.select("p").text()).isEqualTo("€");
    }

    private static String page(String head, String body) {
        return "<html><head>" + head + "</head><body>" + body + "</body></html>";
    }
}
