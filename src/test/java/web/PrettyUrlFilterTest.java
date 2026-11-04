package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** Drives the filter with a stand-in request (no server needed): what it forwards to, and what it remembers. */
public class PrettyUrlFilterTest {

    /** What happened to one request. */
    private static class Outcome {
        String forwardedTo;                       // null if the request was passed on untouched
        boolean passedThrough;
        final Map<String, Object> attributes = new HashMap<>();
    }

    private static Outcome run(String requestUri) throws Exception {
        Outcome out = new Outcome();

        RequestDispatcher dispatcher = (RequestDispatcher) Proxy.newProxyInstance(
                RequestDispatcher.class.getClassLoader(), new Class<?>[] {RequestDispatcher.class},
                (proxy, method, args) -> null);

        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getContextPath": return "/entropy";
                        case "getRequestURI": return requestUri;
                        case "setAttribute": out.attributes.put((String) args[0], args[1]); return null;
                        case "getRequestDispatcher": out.forwardedTo = (String) args[0]; return dispatcher;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });

        new PrettyUrlFilter().doFilter(request, null, (req, res) -> out.passedThrough = true);
        // the dispatcher's forward() was reached if a target was chosen and the chain was not used
        return out;
    }

    @Test
    public void articleAddressIsForwardedToTheArticlePage() throws Exception {
        Outcome o = run("/entropy/hello-world-jdl2i53q");
        assertEquals("/article.xhtml?slug=hello-world-jdl2i53q", o.forwardedTo);
        assertEquals("/entropy/hello-world-jdl2i53q", o.attributes.get(PrettyUrlFilter.PRETTY_URL));
        assertEquals("/article.xhtml", o.attributes.get(PrettyUrlFilter.PRETTY_VIEW));
        assertFalse(o.passedThrough);
    }

    @Test
    public void listPagesAndProfilesAreForwarded() throws Exception {
        assertEquals("/popular.xhtml", run("/entropy/popular").forwardedTo);
        assertEquals("/following.xhtml", run("/entropy/following").forwardedTo);
        assertEquals("/profile.xhtml?username=alias&tab=followers", run("/entropy/@alias/followers").forwardedTo);
        assertEquals("/tag.xhtml?slug=big-data", run("/entropy/tag/big-data").forwardedTo);
    }

    @Test
    public void searchWordsAreDecodedForThePageButTheRememberedAddressStaysEncoded() throws Exception {
        Outcome o = run("/entropy/search/hello%20world");
        assertEquals("/search.xhtml?q=hello+world", o.forwardedTo);
        assertEquals("/search.xhtml", o.attributes.get(PrettyUrlFilter.PRETTY_VIEW));
        assertEquals("forms post back to this: it must stay a valid address",
                "/entropy/search/hello%20world", o.attributes.get(PrettyUrlFilter.PRETTY_URL));
    }

    @Test
    public void searchWordsWithPercentAndQuestionMarkSurvive() throws Exception {
        // the visitor searched for:  50% off?
        Outcome o = run("/entropy/search/50%25%20off%3F");
        assertEquals("/search.xhtml?q=50%25+off%3F", o.forwardedTo);
        assertEquals("/entropy/search/50%25%20off%3F", o.attributes.get(PrettyUrlFilter.PRETTY_URL));
    }

    @Test
    public void aPlusInTheWordsIsAPlus() throws Exception {
        assertEquals("/search.xhtml?q=c%2B%2B+tips", run("/entropy/search/c%2B%2B%20tips").forwardedTo);
    }

    @Test
    public void theSessionIdIsNotPartOfTheAddress() throws Exception {
        Outcome o = run("/entropy/@alias;jsessionid=ABC123");
        assertEquals("/profile.xhtml?username=alias", o.forwardedTo);
        assertEquals("/entropy/@alias", o.attributes.get(PrettyUrlFilter.PRETTY_URL));
    }

    @Test
    public void thingsThatAreNotOursPassThroughUntouched() throws Exception {
        for (String uri : new String[] {"/entropy/", "/entropy/resources/css/tailwind.css",
                "/entropy/jakarta.faces.resource/primefaces.js.xhtml", "/entropy/article.xhtml", "/entropy/a/b/c"}) {
            Outcome o = run(uri);
            assertTrue(uri, o.passedThrough);
            assertNull(uri, o.forwardedTo);
            assertTrue(uri, o.attributes.isEmpty());
        }
    }

    @Test
    public void brokenEncodingIsPassedOnInsteadOfCrashing() throws Exception {
        Outcome o = run("/entropy/search/100%");
        assertTrue(o.passedThrough);
        assertNull(o.forwardedTo);
    }
}
