package com.googlecode.waitrest;

import com.googlecode.funclate.Model;
import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Strings;
import com.googlecode.utterlyidle.Request;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.ResponseBuilder;
import com.googlecode.utterlyidle.Status;
import org.junit.Test;

import static com.googlecode.totallylazy.Some.some;
import static com.googlecode.utterlyidle.HttpHeaders.CONTENT_TYPE;
import static com.googlecode.utterlyidle.MediaType.TEXT_PLAIN;
import static com.googlecode.utterlyidle.RequestBuilder.get;
import static com.googlecode.utterlyidle.RequestBuilder.put;
import static com.googlecode.utterlyidle.ResponseBuilder.response;
import static com.googlecode.utterlyidle.Status.OK;
import static com.googlecode.waitrest.Renderers.stringTemplateRenderer;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;

public class WaitressTest {
    private Kitchen kitchen = new Kitchen();
    private Waitress waitress = new Waitress(kitchen, null);

    @Test
    public void serveRequestResponseOrder() {
        Request request = get("/cheese").build();
        Response response = response(OK).entity("cheese").build();

        waitress.takeOrder(request.toString(), response.toString());

        assertThat(waitress.serveGetOrder(request).toString(), is(response.toString()));
    }

    @Test
    public void serveRequestResponseOrderWithQueryParams() {
        Request requestWithQueryParam = get("/cheese").query("type", "cheddar").build();
        Request requestWithoutQueryParam = get("/cheese").build();
        Response response = response(OK).entity("cheddar").build();

        waitress.takeOrder(requestWithQueryParam.toString(), response.toString());

        assertThat(waitress.serveGetOrder(requestWithQueryParam).toString(), is(response.toString()));
        assertThat(waitress.serveGetOrder(requestWithoutQueryParam).status(), is(Status.NOT_FOUND));
    }

    @Test
    public void serveRequestOrder() {
        waitress.takeOrder(put("/cheese").header(CONTENT_TYPE, TEXT_PLAIN).entity("cheese").build());

        assertThat(waitress.serveGetOrder(get("/cheese").build()).toString(), is(response(OK).header(CONTENT_TYPE, TEXT_PLAIN).entity("cheese").build().toString()));
    }

    @Test
    public void putOverwrites() {
        waitress.takeOrder(put("/cheese").header(CONTENT_TYPE, TEXT_PLAIN).entity("wensleydale").build());
        waitress.takeOrder(put("/cheese").header(CONTENT_TYPE, TEXT_PLAIN).entity("brie").build());
        assertThat(waitress.serveGetOrder(get("/cheese").build()).toString(), is(response(OK).header(CONTENT_TYPE, TEXT_PLAIN).entity("brie").build().toString()));
    }

    @Test
    public void importOrders() throws Exception {
        String orders = Strings.toString(getClass().getResourceAsStream("export.txt"));
        
        assertThat(waitress.importOrders(orders).contains("2 orders imported"), is(true));
    }

    @Test
    public void allOrders() throws Exception {
        waitress.takeOrder("GET http://someserver:1234/some/path HTTP/1.1", "HTTP/1.1 200 OK\n\n<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"><url>http://someserver:1234/foo</url></feed>");
        waitress.takeOrder("GET http://someserver:1234/some/path HTTP/1.1", "HTTP/1.1 200 OK\n\n<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"><url>http://unrelatedServer:1234/foo</url></feed>");
        String response = stringTemplateRenderer("all").render((Model) waitress.allOrders().entity().value());
        assertThat(response, containsString("GET /some/path"));
        assertThat(response, containsString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"><url>http://unrelatedServer:1234/foo</url></feed>"));
    }
}
